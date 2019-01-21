package edu.mcw.rgd;

import edu.mcw.rgd.log.RGDSpringLogger;
import edu.mcw.rgd.process.Utils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.FileSystemResource;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author KPulakanti
 * @since Jun 29, 2010
 */
public class MouseQtlManager {

    FileDownloader downloader;
    QtlDataLoader qtlDataLoader;

    Logger log = Logger.getLogger("core");
    private String version;

    public static void main(String[] args) throws Exception {

        DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
        new XmlBeanDefinitionReader(bf).loadBeanDefinitions(new FileSystemResource("properties/AppConfigure.xml"));
        MouseQtlManager loader = (MouseQtlManager) (bf.getBean("loader"));

        try {
            loader.run();
        } catch( Exception e ) {
            Utils.printStackTrace(e, loader.log);
            throw new Exception(e);
        }
    }

    void run() throws Exception {
        log.info(getVersion());
        log.info("   "+qtlDataLoader.getDbInfo());

        long tmStart = System.currentTimeMillis();
        SimpleDateFormat sdt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        log.info("   started at "+sdt.format(new Date(tmStart)));

        // download input files and store them in local data folder
        downloader.run();

        // load file names to be processed
        String fileNameAllele = downloader.getAlleleFileCopy();
        String fileNameMrkList = downloader.getMrkListFileCopy();
        String fileNameCoordinate = downloader.getCoordFileCopy();

        // parse allele file
        FileAlleleParser alleleParser = new FileAlleleParser();
        Map<String, QtlData> qtlDataMap = new HashMap<String, QtlData>();
        alleleParser.setQtlDataMap(qtlDataMap);
        int lineCount = alleleParser.parse(fileNameAllele);
        log.info("File "+fileNameAllele+": lines read "+lineCount);

        // parse mrk list file
        FileMrkListParser mrkListParser = new FileMrkListParser();
        mrkListParser.setQtlDataMap(qtlDataMap);
        lineCount = mrkListParser.parse(fileNameMrkList);
        log.info("File "+fileNameMrkList+": lines read "+lineCount);

        // parse coord list file
        FileCoordParser coordParser = new FileCoordParser();
        coordParser.setQtlDataMap(qtlDataMap);
        lineCount = coordParser.parse(fileNameCoordinate );
        log.info("File "+fileNameCoordinate+": lines read "+lineCount);

        // display map contents
        for( Map.Entry<String, QtlData> entry: qtlDataMap.entrySet() ) {

            log.debug("QC "+entry.getKey()+" : "+entry.getValue());

            QtlData qtlData = entry.getValue();
            qtlDataLoader.load(qtlData);
        }

        long tmEnd = System.currentTimeMillis();

        log.info("========SUMMARY=======");
        log.info("qtls processed: "+qtlDataMap.size());
        log.info("new qtls added: "+qtlDataLoader.getNewQtlCount());
        log.info("qtls updated: "+qtlDataLoader.getUpdatedQtlCount());

        log.info("qtls with valid cM coordinates: "+qtlDataLoader.getcMMapCount());
        log.info("qtls with valid genomic coordinates: "+qtlDataLoader.getbPMapCount());
        if( qtlDataLoader.getMapPosInserted()>0 )
            log.info("qtls with inserted map positions: "+qtlDataLoader.getMapPosInserted());
        if( qtlDataLoader.getMapPosDeleted()>0 )
            log.info("qtls with deleted coordinates: "+qtlDataLoader.getMapPosDeleted());
        if( qtlDataLoader.getMapPosUpdated()>0 )
            log.info("qtls with updated coordinates: "+qtlDataLoader.getMapPosUpdated());
        log.info("PubMed ids processed: "+qtlDataLoader.getPmIdsProcessed());
        log.info("new PubMed ids added: "+qtlDataLoader.getNewPmIdsAdded());
        log.info("MP ids processed: "+qtlDataLoader.getMpIdsProcessed());
        log.info("new MP ids added: "+qtlDataLoader.getNewMpIdsAdded());
        log.info("MP ids not found in RGD: "+qtlDataLoader.getMpIdsUnknown());
        log.info("pipeline run time: "+ Utils.formatElapsedTime(tmStart, tmEnd));

        log.info("========DONE=======\n");

        logSummaryIntoRgdSpringLogger((tmEnd-tmStart)/1000, qtlDataMap.size());
    }

    void logSummaryIntoRgdSpringLogger(long timeToExecute, int qtlsProcessed) throws Exception {

        RGDSpringLogger rgdLogger = new RGDSpringLogger();
        String subsystem = "mouseQtl";
        rgdLogger.log(subsystem, "timeToExecute", timeToExecute);
        rgdLogger.log(subsystem, "qtlsProcessed", qtlsProcessed);
        rgdLogger.log(subsystem, "qtlsAdded", qtlDataLoader.getNewQtlCount());
        rgdLogger.log(subsystem, "qtlsUpdated", qtlDataLoader.getUpdatedQtlCount());
        rgdLogger.log(subsystem, "validcMCoords", qtlDataLoader.getcMMapCount());
        rgdLogger.log(subsystem, "validGenomicCoords", qtlDataLoader.getbPMapCount());
        rgdLogger.log(subsystem, "pubmedIdsProcessed", qtlDataLoader.getPmIdsProcessed());
        rgdLogger.log(subsystem, "MPIdsProcessed", qtlDataLoader.getMpIdsProcessed());
    }

    public FileDownloader getDownloader() {
        return downloader;
    }

    public void setDownloader(FileDownloader downloader) {
        this.downloader = downloader;
    }

    public QtlDataLoader getQtlDataLoader() {
        return qtlDataLoader;
    }

    public void setQtlDataLoader(QtlDataLoader qtlDataLoader) {
        this.qtlDataLoader = qtlDataLoader;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
    }
}
