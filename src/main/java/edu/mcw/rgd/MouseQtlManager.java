package edu.mcw.rgd;

import edu.mcw.rgd.log.RGDSpringLogger;
import edu.mcw.rgd.process.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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

    Logger log = LogManager.getLogger("status");
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
        log.info("  "+fileNameAllele+": lines read "+Utils.formatThousands(lineCount));

        // parse mrk list file
        FileMrkListParser mrkListParser = new FileMrkListParser();
        mrkListParser.setQtlDataMap(qtlDataMap);
        lineCount = mrkListParser.parse(fileNameMrkList);
        log.info("  "+fileNameMrkList+": lines read "+Utils.formatThousands(lineCount));

        // parse coord list file
        FileCoordParser coordParser = new FileCoordParser();
        coordParser.setQtlDataMap(qtlDataMap);
        coordParser.setKnownGenomeBuilds(qtlDataLoader.getGenomicMaps().keySet());

        lineCount = coordParser.parse(fileNameCoordinate );
        log.info("  "+fileNameCoordinate+": lines read "+Utils.formatThousands(lineCount));

        // display map contents
        for( Map.Entry<String, QtlData> entry: qtlDataMap.entrySet() ) {

            log.debug("QC "+entry.getKey()+" : "+entry.getValue());

            QtlData qtlData = entry.getValue();
            qtlDataLoader.load(qtlData);
        }

        log.info("========SUMMARY=======");
        log.info("qtls processed: "+Utils.formatThousands(qtlDataMap.size()));
        if( qtlDataLoader.getNewQtlCount()>0 ) {
            log.info("new qtls added: " + Utils.formatThousands(qtlDataLoader.getNewQtlCount()));
        }
        if( qtlDataLoader.getUpdatedQtlCount()>0 ) {
            log.info("qtls updated: " + Utils.formatThousands(qtlDataLoader.getUpdatedQtlCount()));
        }

        log.info("qtls with valid cM coordinates: "+Utils.formatThousands(qtlDataLoader.getcMMapCount()));
        log.info("qtls with valid genomic coordinates: "+Utils.formatThousands(qtlDataLoader.getbPMapCount()));
        if( qtlDataLoader.getMapPosInserted()>0 )
            log.info("qtls with inserted map positions: "+Utils.formatThousands(qtlDataLoader.getMapPosInserted()));
        if( qtlDataLoader.getMapPosDeleted()>0 )
            log.info("qtls with deleted coordinates: "+Utils.formatThousands(qtlDataLoader.getMapPosDeleted()));
        if( qtlDataLoader.getMapPosUpdated()>0 )
            log.info("qtls with updated coordinates: "+Utils.formatThousands(qtlDataLoader.getMapPosUpdated()));
        log.info("PubMed ids processed: "+Utils.formatThousands(qtlDataLoader.getPmIdsProcessed()));
        if( qtlDataLoader.getNewPmIdsAdded()>0 ) {
            log.info("new PubMed ids added: " + Utils.formatThousands(qtlDataLoader.getNewPmIdsAdded()));
        }
        log.info("MP ids processed: "+Utils.formatThousands(qtlDataLoader.getMpIdsProcessed()));
        if( qtlDataLoader.getNewMpIdsAdded()>0 ) {
            log.info("new MP ids added: " + Utils.formatThousands(qtlDataLoader.getNewMpIdsAdded()));
        }
        if( qtlDataLoader.getMpIdsUnknown()>0 ) {
            log.info("MP ids not found in RGD: " + Utils.formatThousands(qtlDataLoader.getMpIdsUnknown()));
        }

        long tmEnd = System.currentTimeMillis();
        log.info("=====   DONE   time elapsed "+ Utils.formatElapsedTime(tmStart, tmEnd));
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
