package edu.mcw.rgd;

import org.apache.log4j.Logger;

/**
 * @author KPulakanti
 * @since Jun 29, 2010
 */
public class FileDownloader {

    private String ftpServer;
    private String ftpDirectory;
    private String fileAllele;
    private String fileMrkList;
    private String fileCoordinate;

    private String fileAlleleLocal;
    private String fileMrkListLocal;
    private String fileCoordinateLocal;

    String dataDir = "data/"; // output data directory

    Logger log = Logger.getLogger("core");

    static public void main(String[] args) throws Exception {
        FileDownloader fd = new FileDownloader();
        fd.run();
    }

    /**
     * download all files from MGD ftp site
     * @return true if all files have been downloaded successfully; false otherwise
     */
    public boolean run() throws Exception {

        fileAlleleLocal = download(fileAllele);
        fileMrkListLocal = download(fileMrkList);
        fileCoordinateLocal = download(fileCoordinate);
        return true;
    }
    // Downloads all the three files in a local system from FTP server.

    public String download(String filename) throws Exception {

        edu.mcw.rgd.process.FileDownloader downloader = new edu.mcw.rgd.process.FileDownloader();
        downloader.setExternalFile(ftpServer + ftpDirectory + filename);
        downloader.setLocalFile(dataDir+filename+".gz");
        downloader.setAppendDateStamp(true);
        downloader.setUseCompression(true);

        String localFile = downloader.download();
        log.warn("Downloaded file "+filename);

        return localFile;
    }

    // return full path to the current copy of allele file
    public String getAlleleFileCopy() {
        return this.fileAlleleLocal;
    }

    // return full path to the current copy of mrk list file
    public String getMrkListFileCopy() {
        return this.fileMrkListLocal;
    }

    // return full path to the current copy of coord file
    public String getCoordFileCopy() {
        return this.fileCoordinateLocal;
    }

    public String getFtpServer() {
        return ftpServer;
    }

    public void setFtpServer(String ftpServer) {
        this.ftpServer = ftpServer;
    }

    public String getFtpDirectory() {
        return ftpDirectory;
    }

    public void setFtpDirectory(String directory) {
        this.ftpDirectory = directory;
    }

    public String getFileAllele() {
        return fileAllele;
    }

    public void setFileAllele(String fileAllele) {
        this.fileAllele = fileAllele;
    }

    public String getFileMrkList() {
        return fileMrkList;
    }

    public void setFileMrkList(String fileMrkList) {
        this.fileMrkList = fileMrkList;
    }

    public String getFileCoordinate() {
        return fileCoordinate;
    }

    public void setFileCoordinate(String fileCoordinate) {
        this.fileCoordinate = fileCoordinate;
    }

    public String getDataDir() {
        return dataDir;
    }

    public void setDataDir(String dataDir) {
        this.dataDir = dataDir;
    }
}
