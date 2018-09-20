package edu.mcw.rgd;

import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * @author KPulakanti
 * @since Jun 29, 2010
 * reads the text file in tab delimited format, reads the file line by line, and breaks every line
 * int a number of columns
 */
public abstract class FileParser {

    //  key: MGI-ID,  value: QtlData object
    Map<String, QtlData> qtlDataMap;

    /**
     * read the text file in tab delimited format, read it line by line, and extract all columns
     * @param fileName path name to file
     * @return number of lines read
     */
    public int parse(String fileName) throws Exception {

        Logger log = Logger.getLogger("core");
        log.info("started parsing file "+fileName);

        String line = null;
        int lineCount = 0;

        InputStreamReader fr = null;
        try{
            fr = new InputStreamReader(new GZIPInputStream(new FileInputStream(fileName)));
            BufferedReader br = new BufferedReader(fr);
            while((line = br.readLine())!= null){
                lineCount ++;

                // \t - escape sequence for TABULATION character
                String[] columns = line.split("\t", -1);
                parseLine(columns);
            }
        }
        finally {
            if( fr!=null ) {
                // close the file if it is open
                try { fr.close(); } catch(Exception e){}
            }
        }
        log.info("finished parsing file "+fileName);
        return lineCount;
    }

    // must be implemented by derived classes
    public abstract void parseLine(String[] columns) throws Exception;

    public Map<String, QtlData> getQtlDataMap() {
        return qtlDataMap;
    }

    public void setQtlDataMap(Map<String, QtlData> qtlDataMap) {
        this.qtlDataMap = qtlDataMap;
    }

}
