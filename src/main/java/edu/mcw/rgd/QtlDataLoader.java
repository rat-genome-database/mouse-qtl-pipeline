package edu.mcw.rgd;

import edu.mcw.rgd.datamodel.MapData;
import edu.mcw.rgd.datamodel.QTL;
import edu.mcw.rgd.datamodel.RgdId;
import edu.mcw.rgd.datamodel.XdbId;
import edu.mcw.rgd.datamodel.ontology.Annotation;
import edu.mcw.rgd.datamodel.ontologyx.Term;
import edu.mcw.rgd.process.Utils;
import edu.mcw.rgd.process.sync.MapDataSyncer;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: KPulakanti
 * Date: Jul 21, 2010
 * Time: 4:35:59 PM
 */
public class QtlDataLoader {

    public final int CM_MAP_KEY = 31; // map_key for cM map
    //public final int ASSEMBLY37_MAP_KEY = 18; // map key for current reference assembly v 37
    public final int ASSEMBLY38_MAP_KEY = 35; // map key for current reference assembly v 38
    public final int MAPS_DATA_POSITION_METHOD_ADJUSTED = 5; // Positioned by peak marker with size adjusted to avg qtl size for species
    public final int MAPS_DATA_POSITION_METHOD_IMPORTED = 6; // Position imported from MGD database as it is -- should not be modified during loading

    private String evidenceCode;
    private int createdBy;// user id from DSS.USERS table to 'sign' automatic annotations done by the pipeline
    private String aspect;
    private int refRgdId;
    private String dataSrc;

    int minQtlSize;
    int avgQtlSize;

    MouseQtlDAO mouseQtlDAO = new MouseQtlDAO();

    int newQtlCount = 0; // count of new qtls added to database
    int updatedQtlCount = 0; // count of existing qtls updated in database

    int cmMapCount = 0; // count of QtlData objects with valid cM coordinates
    int bpMapCount = 0; // count of QtlData objects with valid genomic coordinates (bp) on reference assembly
    int mapPosInserted = 0;
    int mapPosUpdated = 0;
    int mapPosDeleted = 0;

    int pmIdsProcessed = 0; // count of PubMed ids processed by the pipeline
    int newPmIdsAdded = 0; // count of new PubMed ids added to RGD
    int mpIdsProcessed = 0; // count of MP: ids processed by the pipeline
    int newMpIdsAdded = 0; // count of new MP: ids added to RGD
    int mpIdsUnknown = 0; // count of MP: ids unknown in RGD

    // notes for current record
    SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");

    // load this particular qtl data object into database
    public void load(QtlData data) throws Exception {

        // validate chromosome fields
        if( data.getChromosome()==null && data.getChrnum()!=null )
            data.setChromosome(data.getChrnum());

        // create notes string
        data.setNotes("Created by mouse qtl pipeline at " + sdf.format(new java.util.Date()));

        // check if the qtl identified by symbol is already in the database?
        QTL qtl = mouseQtlDAO.getMouseQtlBySymbol(data.getSymbol());
        boolean isNewQtl = false; // new qtl has been added to RGD
        if (qtl == null) {
            // there is no such qtl in rgd database
            // create a new mouse qtl in rgd db
            qtl = mouseQtlDAO.createMouseQtl(data);

            newQtlCount++;

            isNewQtl = true;
        }

        updateReferences(qtl);

        updateMapPositions(data, qtl);

        updateMgdId(data, qtl);

        updatePubmedId(data, qtl);

        insertPhenotypeIds(data, qtl);

        // if there were any updates for the existing qtl,
        // then update RGD_IDS.LAST_MODIFIED_DATE and QTLS.LAST_MODIFIED_DATE
        if( !isNewQtl && data.getUpdateCount()>0 ) {
            mouseQtlDAO.updateLastModifiedDate(qtl.getRgdId());

            updatedQtlCount++;
        }
    }

    void updateReferences(QTL qtl) throws Exception {

        List refs = mouseQtlDAO.getQtlReferences(qtl.getRgdId());
        if( refs!=null && refs.size()>0 ) {
            // do nothing, we already have references
        }
        else {
            // add a new reference to MGI
            mouseQtlDAO.addQtlReference(qtl.getRgdId(), getRefRgdId());
        }
    }

    void updateMapPositions(QtlData data, QTL qtl) throws Exception {

        MapDataSyncer syncer = new MapDataSyncer();

        // load cM absolute position from QtlData object
        // and validate it
        Double absPos;
        if ( data.getCmposition()==null
          || data.getCmposition().contains("syntenic")
          || data.getCmposition().contains("null")
          || data.getCmposition().contains("N/A") )
            absPos = null;
        else
            absPos = Double.parseDouble(data.getCmposition());

        // update counts for cM map
        if( (absPos!=null && (absPos>0.99 || absPos<-0.99) ) ||
            (data.getCmposition()!=null && data.getCmposition().contains("syntenic") && !data.getChrnum().isEmpty()) ){
            this.cmMapCount++;

            MapData md = new MapData();
            md.setChromosome(data.getChrnum());
            md.setAbsPosition(absPos);
            if( absPos==null )
                md.setFishBand("syntenic");
            md.setRgdId(qtl.getRgdId());
            md.setMapKey(CM_MAP_KEY);
            md.setNotes(data.getNotes());
            md.setSrcPipeline("MOUSEQTL");

            syncer.addIncomingObject(md);
        }


        // genomic assembly 38: validate start & stop positions
        if( Utils.stringsAreEqual(data.getGenomeBuild(), "GRCm38") ) {
            MapData md = new MapData();
            md.setChromosome(data.getChromosome());
            md.setRgdId(qtl.getRgdId());
            md.setMapKey(ASSEMBLY38_MAP_KEY);
            md.setNotes(data.getNotes());

            if (data.getStart()==null || data.getStart().trim().length()==0 || data.getStart().startsWith("null") ||
                data.getStop()==null || data.getStop().trim().length()==0 || data.getStop().startsWith("null")) {
            }
            else {
                md.setStartPos(Integer.parseInt(data.getStart()));
                md.setStopPos(Integer.parseInt(data.getStop()));

                // ensure start_pos <= stop_pos
                if( md.getStartPos()>md.getStopPos() ) {
                    // swap start with stop  pos to satisfy the constraint
                    int hiPos = md.getStartPos();
                    int loPos = md.getStopPos();
                    md.setStartPos(loPos);
                    md.setStopPos(hiPos);
                }

                md.setStrand(data.getStrand());
                md.setSrcPipeline("MOUSEQTL");

                adjustToAvgQtlSize(md);

                // update counts for genomic map 38
                this.bpMapCount++;

                syncer.addIncomingObject(md);
            }
        }


        // load position info from rgd db
        List<MapData> mapDataInRgd = mouseQtlDAO.getMapData(qtl.getRgdId());
        syncer.setObjectsInRgd(mapDataInRgd);

        syncer.qc("mapData");


        // insert map data
        List<MapData> mds = syncer.getObjectsForInsert();
        if( !mds.isEmpty() ) {
            for( MapData m: mds ) {
                mouseQtlDAO.insertMapData(m);
                data.incrementUpdateCount();
                mapPosInserted++;
            }
        }
        // update map data
        mds = syncer.getObjectsForUpdate();
        if( !mds.isEmpty() ) {
            for( MapData m: mds ) {
                mouseQtlDAO.updateMapData(m);
                data.incrementUpdateCount();
                mapPosUpdated++;
            }
        }
        // delete map data
        mds = syncer.getObjectsForDelete();
        if( !mds.isEmpty() ) {
            for( MapData m: mds ) {
                mouseQtlDAO.deleteMapData(m);
                data.incrementUpdateCount();
                mapPosDeleted++;
            }
        }
    }

    void updateMgdId(QtlData data, QTL qtl) throws Exception {
        //   check for RGD ID s in the DB(XDB table) and compare that with the incoming RGD ID
        // If it is the same do not add any row
        XdbId xdbIdFilter = new XdbId();
        // set filter to QTL_RGD_ID and XDB_KEY=MGD_ID and ACC_ID equals our QTL MGI ID
        xdbIdFilter.setRgdId(qtl.getRgdId());
        xdbIdFilter.setXdbKey(XdbId.XDB_KEY_MGD);
        xdbIdFilter.setAccId(data.getMgiId());
        List<XdbId> xdbIDInRgd = mouseQtlDAO.getXdbIds(xdbIdFilter);

        // IF THE LIST 'xdbIdInRgd' is empty
        //    THEN insert new row into RGD_ACC_XDB table
        // ELSE  (there is at least one row there)
        //    DO NOTHING
        // END IF
        //insert the xdbId s in the Rgd DB - RGD_ACC_XDB table

        if (xdbIDInRgd.isEmpty()) {
            //  If the RGD ID is not present in the RGD_ACC_XDB table,then add a row which takes the QTL RGDID
            // insert new row
            XdbId row = new XdbId();
            row.setRgdId(qtl.getRgdId());
            row.setXdbKey(XdbId.XDB_KEY_MGD);
            row.setAccId(data.getMgiId());
            row.setLinkText(data.getMgiId());
            row.setSrcPipeline("MouseQtl");
            row.setNotes(data.getNotes());

            mouseQtlDAO.insertXdb(row);

            data.incrementUpdateCount();
        }
    }

    void updatePubmedId(QtlData data, QTL qtl) throws Exception {

        // for every pubmed id
        for( String pubMedId: data.getPmIdList() ) {

            this.pmIdsProcessed++;

            //   check for RGD ID s in the DB(XDB table) and compare that with the incoming RGD ID
            // If it is the same do not add any row
            XdbId xdbIdFilter1 = new XdbId();
            // set filter to QTL_RGD_ID and XDB_KEY_PUBMED and ACC_ID equals our QTL PMID
            xdbIdFilter1.setRgdId(qtl.getRgdId());
            xdbIdFilter1.setXdbKey(XdbId.XDB_KEY_PUBMED);
            xdbIdFilter1.setAccId(pubMedId);
            List<XdbId> xdbIDinRgd = mouseQtlDAO.getXdbIds(xdbIdFilter1);

            // IF THE LIST 'xdbIdInRgd' is empty
            //    THEN insert new row into RGD_ACC_XDB table
            // ELSE  (there is at least one row there)
            //    DO NOTHING
            // END IF
            //insert the xdbId s in the Rgd DB - RGD_ACC_XDB table

            if (xdbIDinRgd.isEmpty()) {
                //  If the RGD ID is not present in the RGD_ACC_XDB table,then add a row which takes the QTL RGDID
                // insert new row
                XdbId row = new XdbId();
                row.setRgdId(qtl.getRgdId());
                row.setXdbKey(XdbId.XDB_KEY_PUBMED);
                row.setAccId(pubMedId);
                //row.setLinkText(data.getMgiId());
                row.setSrcPipeline("MouseQtl");
                row.setNotes(data.getNotes());

                mouseQtlDAO.insertXdb(row);

                this.newPmIdsAdded++;

                data.incrementUpdateCount();
            }
        }
    }

    void insertPhenotypeIds(QtlData data, QTL qtl) throws Exception {

        //   check for RGD ID s in the DB(FULL_ANNOT table) and compare that with the incoming RGD ID
        //If it appears same in the table,then do nothing(or do not add any row)

        // check every MP id
        for (String mpId : data.getMpIdList()) {

            // skip empty mp ids
            if( mpId.trim().length()==0 )
                continue;

            this.mpIdsProcessed++;

            Annotation anno = new Annotation();
            anno.setAnnotatedObjectRgdId(qtl.getRgdId());
            anno.setEvidence(getEvidenceCode());
            anno.setTermAcc(mpId);
            anno.setCreatedBy(getCreatedBy());
            anno.setLastModifiedBy(getCreatedBy());
            anno.setXrefSource(data.getPubMedIdsAsString());
            anno.setObjectName(data.getName());
            anno.setAspect(getAspect());
            anno.setRefRgdId(getRefRgdId());
            anno.setRgdObjectKey(RgdId.OBJECT_KEY_QTLS);
            anno.setObjectSymbol(data.getSymbol());
            anno.setDataSrc(getDataSrc());
            anno.setAnnotatedObjectRgdId(qtl.getRgdId());
            anno.setNotes("Created by mouse qtl pipeline");

            Term term = mouseQtlDAO.getTerm(anno.getTermAcc());
            if (term != null) {
                anno.setTerm(term.getTerm());
            }
            else {
                // unexpected: there is no such term in RGD -- annotation cannot be created
                this.mpIdsUnknown++;
            }

            Annotation a = mouseQtlDAO.getPhenotypeIds(anno);
            if( a==null ) {

                // new annotation has to be added
                mouseQtlDAO.insertPhenotypeIds(anno);
                this.newMpIdsAdded++;
                data.incrementUpdateCount();
            }
            else {
                // we have this annotation in RGD -- update its last modified date
                mouseQtlDAO.updatePhenotypeAnnotLastModifiedDate(a.getKey());
            }
        }
    }

    // if the qtl size is less then 10000 bp, adjust it so it will be set to avg qtl size
    void adjustToAvgQtlSize(MapData md) throws Exception {

        // the chromosome must be not null as well as start and stop positions
        if( md.getChromosome()==null || md.getStartPos()==null || md.getStopPos()==null )
            return;

        // current qtl size
        int qtlSize = md.getStopPos()-md.getStartPos()+1;

        // if the qtl size is greater than minimum size, leave as it is
        if( qtlSize>=getMinQtlSize() ) {
            md.setMapsDataPositionMethodId(MAPS_DATA_POSITION_METHOD_IMPORTED);
            return;
        }

        // qtl size less than min qtl size -- adjust it
        md.setStartPos( md.getStartPos() - getAvgQtlSize()/2 );
        // qtl starting position cannot be less than 1
        if( md.getStartPos()<= 0 )
            md.setStartPos(1);
        // qtl stop position cannot be greater than chromosome size
        md.setStopPos( md.getStopPos() + getAvgQtlSize()/2 );
        int chrSize = getChromosomeSize(md.getChromosome());
        if( md.getStopPos() > chrSize )
            md.setStopPos(chrSize);
        md.setMapsDataPositionMethodId(MAPS_DATA_POSITION_METHOD_ADJUSTED);
    }
    
    // get chromosome size on reference assembly
    int getChromosomeSize(String chr) throws Exception {

        return mouseQtlDAO.getChromosomeSize(ASSEMBLY38_MAP_KEY, chr);
    }

    public int getcMMapCount() {
        return cmMapCount;
    }

    public void setcMMapCount(int cMMapCount) {
        this.cmMapCount = cMMapCount;
    }

    public int getbPMapCount() {
        return bpMapCount;
    }

    public void setbPMapCount(int bPMapCount) {
        this.bpMapCount = bPMapCount;
    }

    public int getNewQtlCount() {
        return newQtlCount;
    }

    public void setNewQtlCount(int newQtlCount) {
        this.newQtlCount = newQtlCount;
    }

    public int getPmIdsProcessed() {
        return pmIdsProcessed;
    }

    public void setPmIdsProcessed(int pmIdsProcessed) {
        this.pmIdsProcessed = pmIdsProcessed;
    }

    public int getNewPmIdsAdded() {
        return newPmIdsAdded;
    }

    public void setNewPmIdsAdded(int newPmIdsAdded) {
        this.newPmIdsAdded = newPmIdsAdded;
    }

    public int getMpIdsProcessed() {
        return mpIdsProcessed;
    }

    public void setMpIdsProcessed(int mpIdsProcessed) {
        this.mpIdsProcessed = mpIdsProcessed;
    }

    public int getNewMpIdsAdded() {
        return newMpIdsAdded;
    }

    public void setNewMpIdsAdded(int newMpIdsAdded) {
        this.newMpIdsAdded = newMpIdsAdded;
    }

    public int getMpIdsUnknown() {
        return mpIdsUnknown;
    }

    public void setMpIdsUnknown(int mpIdsUnknown) {
        this.mpIdsUnknown = mpIdsUnknown;
    }

    public int getUpdatedQtlCount() {
        return updatedQtlCount;
    }

    public void setUpdatedQtlCount(int updatedQtlCount) {
        this.updatedQtlCount = updatedQtlCount;
    }

    public int getMinQtlSize() {
        return minQtlSize;
    }

    public void setMinQtlSize(int minQtlSize) {
        this.minQtlSize = minQtlSize;
    }

    public int getAvgQtlSize() {
        return avgQtlSize;
    }

    public void setAvgQtlSize(int avgQtlSize) {
        this.avgQtlSize = avgQtlSize;
    }

    public void setEvidenceCode(String evidenceCode) {
        this.evidenceCode = evidenceCode;
    }

    public String getEvidenceCode() {
        return evidenceCode;
    }

    public void setCreatedBy(int createdBy) {
        this.createdBy = createdBy;
    }

    public int getCreatedBy() {
        return createdBy;
    }

    public void setAspect(String aspect) {
        this.aspect = aspect;
    }

    public String getAspect() {
        return aspect;
    }

    public void setRefRgdId(int refRgdId) {
        this.refRgdId = refRgdId;
    }

    public int getRefRgdId() {
        return refRgdId;
    }

    public void setDataSrc(String dataSrc) {
        this.dataSrc = dataSrc;
    }

    public String getDataSrc() {
        return dataSrc;
    }

    public int getMapPosInserted() {
        return mapPosInserted;
    }

    public void setMapPosInserted(int mapPosInserted) {
        this.mapPosInserted = mapPosInserted;
    }

    public int getMapPosUpdated() {
        return mapPosUpdated;
    }

    public void setMapPosUpdated(int mapPosUpdated) {
        this.mapPosUpdated = mapPosUpdated;
    }

    public int getMapPosDeleted() {
        return mapPosDeleted;
    }

    public void setMapPosDeleted(int mapPosDeleted) {
        this.mapPosDeleted = mapPosDeleted;
    }
}

