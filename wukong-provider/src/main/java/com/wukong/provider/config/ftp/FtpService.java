package com.wukong.provider.config.ftp;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * FTP工具类
 * @author dq
 */

@Slf4j
@Data
@Component
@PropertySource(value = "classpath:conf/ftp.properties", ignoreResourceNotFound = true)
@Configuration
public class FtpService {

    private FTPClient ftpClient = null;

    @Value("${ftp.host}")
    private String server;
    @Value("${ftp.port}")
    private int port;
    @Value("${ftp.username}")
    private String userName;
    @Value("${ftp.password}")
    private String userPassword;

    /**
     * 连接服务器
     * @return 连接成功与否 true:成功， false:失败
     */
    @PostConstruct
    public boolean open() {
        if (ftpClient != null && ftpClient.isConnected()) {
            return true;
        }
        try {
            ftpClient = new FTPClient();
            ftpClient.connect(this.server, this.port);
            ftpClient.login(this.userName, this.userPassword);
            ftpClient.setDefaultTimeout(1000 * 60);
            //setFtpClient(ftpClient);
            // 检测连接是否成功
            int reply = ftpClient.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
                this.close();
                log.error("FTP server refused connection.");
                return false;
            }
            log.info("open FTP success:" + this.server + ";port:" + this.port + ";name:" + this.userName
                    + ";pwd:" + this.userPassword);
            return true;
        } catch (Exception ex) {
            log.error("连接ftp服务器失败， {}", ex.toString());
            this.close();
            return false;
        }
    }

    /**
     * 切换到父目录
     * @return 切换结果 true：成功， false：失败
     */
    private boolean changeToParentDir() {
        try {
            open();
            return ftpClient.changeToParentDirectory();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 改变当前目录到指定目录
     * @param dir 目的目录
     * @return 切换结果 true：成功，false：失败
     */
    private boolean cd(String dir) {
        try {
            open();
            return ftpClient.changeWorkingDirectory(dir);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 获取目录下所有的文件名称
     *
     * @param filePath 指定的目录
     * @return 文件列表,或者null
     */
    private FTPFile[] getFileList(String filePath) {
        try {
            open();
            return ftpClient.listFiles(filePath);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 层层切换工作目录
     * @param ftpPath 目的目录
     * @return 切换结果
     */
    public boolean changeDir(String ftpPath) {
        if (!open()) {
            return false;
        }
        try {
            // 将路径中的斜杠统一
            char[] chars = ftpPath.toCharArray();
            StringBuffer sbStr = new StringBuffer(256);
            for (int i = 0; i < chars.length; i++) {
                if ('\\' == chars[i]) {
                    sbStr.append('/');
                } else {
                    sbStr.append(chars[i]);
                }
            }
            ftpPath = sbStr.toString();
            if (ftpPath.indexOf('/') == -1) {
                // 只有一层目录
                ftpClient.changeWorkingDirectory(new String(ftpPath.getBytes(), StandardCharsets.ISO_8859_1));
            } else {
                // 多层目录循环创建
                String[] paths = ftpPath.split("/");
                for (int i = 0; i < paths.length; i++) {
                    ftpClient.changeWorkingDirectory(new String(paths[i].getBytes(), StandardCharsets.ISO_8859_1));
                }
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 循环创建目录，并且创建完目录后，设置工作目录为当前创建的目录下
     * @param ftpPath 需要创建的目录
     * @return
     */
    public boolean mkDir(String ftpPath) {
        if (!open()) {
            return false;
        }
        try {
            // 将路径中的斜杠统一
            char[] chars = ftpPath.toCharArray();
            StringBuffer sbStr = new StringBuffer(256);
            for (int i = 0; i < chars.length; i++) {
                if ('\\' == chars[i]) {
                    sbStr.append('/');
                } else {
                    sbStr.append(chars[i]);
                }
            }
            ftpPath = sbStr.toString();
            log.info("ftpPath:" + ftpPath);
            if (ftpPath.indexOf('/') == -1) {
                // 只有一层目录
                ftpClient.makeDirectory(new String(ftpPath.getBytes(), StandardCharsets.ISO_8859_1));
                ftpClient.changeWorkingDirectory(new String(ftpPath.getBytes(),  StandardCharsets.ISO_8859_1));
            } else {
                // 多层目录循环创建
                String[] paths = ftpPath.split("/");
                for (int i = 0; i < paths.length; i++) {
                    ftpClient.makeDirectory(new String(paths[i].getBytes(),  StandardCharsets.ISO_8859_1));
                    ftpClient.changeWorkingDirectory(new String(paths[i].getBytes(),  StandardCharsets.ISO_8859_1));
                }
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 上传文件到FTP服务器
     * @param localDirectoryAndFileName 本地文件目录和文件名
     * @param ftpFileName 上传到服务器的文件名
     * @param ftpDirectory FTP目录如:/path1/pathb2/,如果目录不存在会自动创建目录
     * @return
     */
    public synchronized boolean upload(String localDirectoryAndFileName, String ftpFileName, String ftpDirectory) {
        if(!open()){
            return false;
        }
        boolean flag = false;
        if (ftpClient != null) {
            File srcFile = new File(localDirectoryAndFileName);
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(srcFile);
                ftpClient.enterLocalPassiveMode();
                // 创建目录
                ftpClient.changeWorkingDirectory(ftpDirectory);
                ftpClient.setBufferSize(10240);
                ftpClient.setControlEncoding("UTF-8");

                // 设置文件类型（二进制）
                ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);
                // 上传
                flag = ftpClient.storeFile(new String(ftpFileName.getBytes(), StandardCharsets.ISO_8859_1), fis);

                if(flag){
                    log.info("上传文件成功，本地文件名： {}，上传到目录：{}" , localDirectoryAndFileName, ftpDirectory + ftpFileName);
                } else {
                    log.error("上传文件失败，本地文件名： {}，上传到目录：{}" , localDirectoryAndFileName, ftpDirectory + ftpFileName);
                }

            } catch (Exception e) {
                this.close();
                log.error("上传文件失败，本地文件名： {}, cause {}" ,localDirectoryAndFileName , e.toString()   );
                return false;
            } finally {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return flag;
    }


    /**
     * 上传文件到服务器,新上传和断点续传
     *
     * @param remotePath
     *            远程文件名，在上传之前已经将服务器工作目录做了改变
     * @param localPath
     *            本地文件File句柄，绝对路径
     *            需要显示的处理进度步进值
     * @param ftpClient
     *            FTPClient引用
     * @return
     * @throws IOException
     */
    private boolean uploadFile(String remotePath, File localPath,
                               FTPClient ftpClient, long remoteSize) throws IOException {
        // 显示进度的上传
        long step = localPath.length() / 100;
        long process = 0;
        long localreadbytes = 0L;
        RandomAccessFile raf = new RandomAccessFile(localPath, "r");
        OutputStream out = ftpClient.appendFileStream(remotePath);
        //防止除0错误
        if(step==0)
            step = 1;
        // 断点续传
        if (remoteSize > 0) {
            System.out.println("文件存在，可进行断点续传.");
            ftpClient.setRestartOffset(remoteSize);
            process = remoteSize / step;
            raf.seek(remoteSize);
            localreadbytes = remoteSize;
        }
        byte[] bytes = new byte[1024];
        int c;
        while ((c = raf.read(bytes)) != -1) {
            out.write(bytes, 0, c);
            localreadbytes += c;
            if (localreadbytes / step != process) {
                process = localreadbytes / step;
                if (process > 0 && process % 10 == 0) {
                    if (process == 100) {
                        System.out.println(process + "%");
                        System.out.println(" 上传进度:" + process + "%");
                    } else
                        System.out.print(process + "%");
                } else {
                    System.out.print(".");
                }
            }
        }
        out.flush();
        raf.close();
        out.close();
        boolean result = ftpClient.completePendingCommand();
        if (result) {
            log.info("上传文件完成");
            return true;
        } else {
            log.error("上传文件失败");
            return false;
        }
    }


    /**
     * 从FTP服务器上下载文件
     * @param ftpDirectoryAndFileName ftp服务器文件路径，以/dir形式开始
     * @param localDirectoryAndFileName 保存到本地的目录
     * @return
     */
    public boolean get(String ftpDirectoryAndFileName, String localDirectoryAndFileName) {
        if(!open()){
            return false;
        }
        ftpClient.enterLocalPassiveMode(); // Use passive mode as default
        try {
            // 将路径中的斜杠统一
            char[] chars = ftpDirectoryAndFileName.toCharArray();
            StringBuffer sbStr = new StringBuffer(256);
            for (int i = 0; i < chars.length; i++) {
                if ('\\' == chars[i]) {
                    sbStr.append('/');
                } else {
                    sbStr.append(chars[i]);
                }
            }
            ftpDirectoryAndFileName = sbStr.toString();
            String filePath = ftpDirectoryAndFileName.substring(0, ftpDirectoryAndFileName.lastIndexOf("/"));
            String fileName = ftpDirectoryAndFileName.substring(ftpDirectoryAndFileName.lastIndexOf("/") + 1);
            this.changeDir(filePath);
            ftpClient.retrieveFile(new String(fileName.getBytes(), StandardCharsets.ISO_8859_1),
                    new FileOutputStream(localDirectoryAndFileName)); // download
            // file
            log.info(ftpClient.getReplyString()); // check result
            log.info("从ftp服务器上下载文件：" + ftpDirectoryAndFileName + "， 保存到：" + localDirectoryAndFileName);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 返回FTP目录下的文件列表
     * @param pathName
     * @return
     */
    public String[] getFileNameList(String pathName) {
        try {
            open();
            return ftpClient.listNames(pathName);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 返回FTP目录下的文件列表
     * @return
     */
    public String[] getFileNameList() {
        try {
            open();
            return ftpClient.listNames();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 删除FTP上的文件
     * @param ftpDirAndFileName 路径开头不能加/，比如应该是test/filename1
     * @return
     */
    public boolean deleteFile(String ftpDirAndFileName) {
        open();
        if (!ftpClient.isConnected()) {
            return false;
        }
        try {
            return ftpClient.deleteFile(ftpDirAndFileName);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 删除FTP目录
     * @param ftpDirectory
     * @return
     */
    public boolean deleteDirectory(String ftpDirectory) {
        open();
        if (!ftpClient.isConnected()) {
            return false;
        }
        try {
            return ftpClient.removeDirectory(ftpDirectory);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 关闭链接
     */
    public void close() {
        try {
            if (ftpClient != null && ftpClient.isConnected()) {
                ftpClient.disconnect();
            }
            log.info("成功关闭连接，服务器ip:" + this.server + ", 端口:" + this.port);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}