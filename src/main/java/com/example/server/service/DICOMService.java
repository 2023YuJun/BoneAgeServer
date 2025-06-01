package com.example.server.service;

import com.example.server.config.DICOMConfig;
import org.dcm4che3.data.*;
import org.dcm4che3.io.DicomInputStream;
import org.dcm4che3.net.*;
import org.dcm4che3.net.pdu.AAssociateRQ;
import org.dcm4che3.net.pdu.PresentationContext;
import org.dcm4che3.net.service.BasicCEchoSCP;
import org.dcm4che3.net.service.DicomServiceRegistry;
import org.dcm4che3.util.UIDUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class DICOMService {
    private static final String DICOM_DIR = "temp/dicom_files/";
    private static final String PNG_DIR = "temp/images/";
    private static final int CLEANUP_DAYS = 1;

    private final DICOMConfig config;
    private final Executor dicomExecutor;

    @Autowired
    public DICOMService(DICOMConfig config, @Qualifier("dicomTaskExecutor") Executor dicomExecutor) {
        this.config = config;
        this.dicomExecutor = dicomExecutor;
        initDirectories();
    }

    private void initDirectories() {
        try {
            Files.createDirectories(Paths.get(DICOM_DIR));
            Files.createDirectories(Paths.get(PNG_DIR));
        } catch (IOException e) {
            throw new RuntimeException("初始化目录失败: " + e.getMessage());
        }
    }
    
    public List<Attributes> patientSearch(String patientID) {
        try {
            Attributes keys = new Attributes();
            keys.setString(Tag.QueryRetrieveLevel, VR.CS, "PATIENT");
            keys.setString(Tag.PatientID, VR.LO, patientID);
            keys.setString(Tag.PatientSex, VR.CS, "");
            keys.setString(Tag.PatientBirthDate, VR.DA, "");
            return queryWithRetry(keys);
        } catch (RuntimeException e) {
            throw new RuntimeException("患者查询失败", e);
        }
    }

    public List<Attributes> studySearch(String patientID) {
        try {
            Attributes keys = new Attributes();
            keys.setString(Tag.QueryRetrieveLevel, VR.CS, "STUDY");
            keys.setString(Tag.PatientID, VR.LO, patientID);
            keys.setString(Tag.StudyInstanceUID, VR.UI, "");
            keys.setString(Tag.StudyDate, VR.DA, "");
            return queryWithRetry(keys);
        } catch (RuntimeException e) {
            throw new RuntimeException("检查查询失败", e);
        }
    }

    public List<Attributes> seriesSearch(String StudyUID) {
        try {
            Attributes keys = new Attributes();
            keys.setString(Tag.QueryRetrieveLevel, VR.CS, "SERIES");
            keys.setString(Tag.StudyInstanceUID, VR.UI, StudyUID);
            keys.setString(Tag.SeriesInstanceUID, VR.UI, "");
            keys.setString(Tag.SeriesDescription, VR.LO, "");
            return queryWithRetry(keys);
        } catch (RuntimeException e) {
            throw new RuntimeException("序列查询失败", e);
        }
    }

    public List<Attributes> imageSearch(String SeriesUID) {
        try {
            Attributes keys = new Attributes();
            keys.setString(Tag.QueryRetrieveLevel, VR.CS, "IMAGE");
            keys.setString(Tag.SeriesInstanceUID, VR.UI, SeriesUID);
            keys.setString(Tag.SOPInstanceUID, VR.UI, "");
            return queryWithRetry(keys);
        } catch (RuntimeException e) {
            throw new RuntimeException("图像查询失败", e);
        }
    }

    private List<Attributes> queryWithRetry(Attributes keys) {
        final int maxRetries = 3;
        int retryDelay = 1;
        List<Attributes> results = new ArrayList<>();
        Exception lastException = null;

        for (int attempt = 0; attempt < maxRetries; attempt++) {
            Association association = null;

            try {
                association = createAssociation();

                Object lock = new Object();
                AtomicBoolean completed = new AtomicBoolean(false);

                association.cfind(
                    UID.PatientRootQueryRetrieveInformationModelFind,
                    1,
                    keys,
                    null,
                    new DimseRSPHandler(0) {
                        @Override
                        public void onDimseRSP(Association as, Attributes cmd, Attributes data) {
                            int status = cmd.getInt(Tag.Status, -1);
                            if (status == Status.Pending && data != null) { // ff00H
                                System.out.println("Received Dataset: " + data);
                                results.add(data);
                            } else if (status == Status.Success) {
                                synchronized (lock) {
                                    completed.set(true);
                                    lock.notifyAll();
                                }
                            }
                        }
                    }
                );
                synchronized (lock) {
                    while (!completed.get()) {
                        try {
                            lock.wait(30000); // 最多等待60秒
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
                return results;
            } catch (Exception e) {
                lastException = e;
                System.err.printf("查询失败 (尝试 %d/%d): [%s] %s%n",
                        attempt + 1, maxRetries, e.getClass().getSimpleName(), e.getMessage());
                // 打印堆栈跟踪（调试阶段启用）
                e.printStackTrace();
                try { Thread.sleep(retryDelay * 200L); } catch (InterruptedException ignored) {}
                retryDelay *= 2;
            } finally {
                if (association != null) {
                    try {
                        association.release();
                        if (association.getDevice() != null) {
                            association.getDevice().unbindConnections();
                        }
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }
        throw new RuntimeException("所有重试均失败: " + (lastException != null ? lastException.getMessage() : "未知错误"), lastException);
    }

    public String downloadAndConvertToPng(String downloadUrl, String sopUID) {
        Path dicomPath = null;
        try {
            // 下载DICOM文件
            String dicomFileName = sopUID + ".dcm";
            dicomPath = Paths.get(DICOM_DIR, dicomFileName);
            downloadFile(downloadUrl, dicomPath.toString());

            // 转换为PNG
            String pngFileName = sopUID + ".png";
            Path pngPath = Paths.get(PNG_DIR, pngFileName);
            convertDicomToPng(dicomPath.toString(), pngPath.toString());

            // 删除临时DICOM文件
            Files.deleteIfExists(dicomPath);
            return pngPath.toString();
        } catch (IOException e) {
            // 记录错误日志
            System.err.println("文件处理失败 [SOP_UID=" + sopUID + "]: " + e.getMessage());
            // 清理残留文件
            try {
                if (dicomPath != null) Files.deleteIfExists(dicomPath);
            } catch (IOException ex) {
                System.err.println("清理临时文件失败: " + ex.getMessage());
            }
            return null;
        }
    }

    // ----------------------- 工具方法 -----------------------
    private Association createAssociation() throws Exception {
        Device device = new Device(config.getLocalAeTitle());
        Connection localConn = new Connection();
        device.addConnection(localConn);

        device.setExecutor(dicomExecutor);
        device.setScheduledExecutor((ScheduledExecutorService) dicomExecutor);

        ApplicationEntity ae = new ApplicationEntity(config.getLocalAeTitle());
        ae.setAssociationInitiator(true);
        device.addApplicationEntity(ae);
        ae.addConnection(localConn);

        device.bindConnections();

        Connection remoteConn = new Connection();
        remoteConn.setHostname(config.getPacsIp());
        remoteConn.setPort(config.getPacsPort());

        // 构建关联请求
        AAssociateRQ aarq = new AAssociateRQ();
        aarq.setCalledAET(config.getPacsAeTitle());
        aarq.setCallingAET(config.getLocalAeTitle());

        // 添加必要的Presentation Context
        aarq.addPresentationContext(
                new PresentationContext(
                        1,
                        UID.PatientRootQueryRetrieveInformationModelFind,
                        UID.ExplicitVRLittleEndian
                )
        );

        return ae.connect(remoteConn, aarq);
    }

    private void downloadFile(String urlStr, String savePath) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        try (var in = conn.getInputStream();
             var out = Files.newOutputStream(Paths.get(savePath))) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }
    }

    private void convertDicomToPng(String dicomPath, String pngPath) throws IOException {
        try (DicomInputStream dis = new DicomInputStream(new File(dicomPath))) {
            Attributes dataset = dis.readDataset();
            int[] pixels = dataset.getInts(Tag.PixelData);
            int rows = dataset.getInt(Tag.Rows, 0);
            int columns = dataset.getInt(Tag.Columns, 0);
            if (rows == 0 || columns == 0) throw new IOException("无效的DICOM图像尺寸");

            double windowCenter = dataset.getDouble(Tag.WindowCenter, 0);
            double windowWidth = dataset.getDouble(Tag.WindowWidth, 0);
            if (windowWidth == 0) {
                windowWidth = dataset.getInt(Tag.LargestImagePixelValue, 255) -
                        dataset.getInt(Tag.SmallestImagePixelValue, 0);
            }

            BufferedImage image = applyWindowLevel(pixels, rows, columns, windowCenter, windowWidth);
            ImageIO.write(image, "PNG", new File(pngPath));
        }
    }

    private BufferedImage applyWindowLevel(int[] pixels, int rows, int cols, double center, double width) {
        BufferedImage image = new BufferedImage(cols, rows, BufferedImage.TYPE_INT_RGB);
        double min = center - width / 2;
        double max = center + width / 2;
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                int pixel = pixels[y * cols + x];
                double normalized = (pixel - min) / (max - min) * 255;
                int value = (int) Math.max(0, Math.min(255, normalized));
                int rgb = (value << 16) | (value << 8) | value;
                image.setRGB(x, y, rgb);
            }
        }
        return image;
    }

    public String buildDownloadUrl(String studyUID, String seriesUID, String sopUID, String studyDate, String patientID) {
        return String.format(
                "http://%s:%d/WADO/ROX1?requesttype=WADO&studyUID=%s&seriesUID=%s" +
                        "&objectUID=%s&modality=PX&StudyDate=%s&PatientID=%s&contentType=application/dicom",
                config.getPacsIp(),
                config.getDownloadPort(),
                studyUID,
                seriesUID,
                sopUID,
                studyDate,
                patientID
        );
    }

    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupOldFiles() {
        cleanDirectory(DICOM_DIR);
        cleanDirectory(PNG_DIR);
    }

    private void cleanDirectory(String dirPath) {
        File directory = new File(dirPath);
        if (!directory.exists()) return;
        File[] files = directory.listFiles();
        if (files == null) return;

        long cutoff = System.currentTimeMillis() - (CLEANUP_DAYS * 24 * 60 * 60 * 1000L);
        for (File file : files) {
            if (file.lastModified() < cutoff) {
                try { Files.deleteIfExists(file.toPath()); }
                catch (IOException e) { System.err.println("删除失败: " + file.getAbsolutePath()); }
            }
        }
    }
}