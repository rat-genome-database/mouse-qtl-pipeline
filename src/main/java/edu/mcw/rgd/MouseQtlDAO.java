package edu.mcw.rgd;

import edu.mcw.rgd.dao.impl.*;
import edu.mcw.rgd.datamodel.*;
import edu.mcw.rgd.datamodel.ontology.Annotation;
import edu.mcw.rgd.datamodel.ontologyx.Term;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @author KPulakanti
 * @since Jul 21, 2010
 */
public class MouseQtlDAO {

    RGDManagementDAO rgdDAO = new RGDManagementDAO();
    MapDAO mapDAO = new MapDAO();
    XdbIdDAO xdbIdDAO = new XdbIdDAO();
    OntologyXDAO ontologyDAO = new OntologyXDAO();
    AnnotationDAO annotationDAO = new AnnotationDAO();
    AssociationDAO associationDAO = new AssociationDAO();
    QTLDAO qtlDAO = associationDAO.getQtlDAO();

    Logger log = Logger.getLogger("core");
    Logger logAnnots = Logger.getLogger("insertedAnnots");

    public MouseQtlDAO() {
        // log the db username and url
        try {
            log.warn(rgdDAO.getConnectionInfo());
        }
        catch( Exception e ) {
            e.printStackTrace();
        }
    }

    /**
     * get mouse qtl object identified by given symbol; return null if not found
     * @param symbol symbol
     * @return QTL object or null if there is no mouse qtl with such a symbol in the database
     * @throws Exception
     */
    public QTL getMouseQtlBySymbol(String symbol) throws Exception {

        return qtlDAO.getQTLBySymbol(symbol, SpeciesType.MOUSE);
    }

    public QTL createMouseQtl(QtlData data) throws Exception {

        // create new rgd id for the new qtl
        String objectStatus = "ACTIVE";
        RgdId rgdId = rgdDAO.createRgdId(RgdId.OBJECT_KEY_QTLS, objectStatus, data.getNotes(), SpeciesType.MOUSE);

        // create qtl object
        QTL qtl = new QTL();

        //If the incoming chromosome contains a null text then replace it with NULL which has unknown value.
        String chrom = data.getChromosome();
        if(chrom!=null && chrom.startsWith("null"))
            chrom = null;

        qtl.setChromosome(chrom);
        qtl.setNotes(data.getNotes());
        qtl.setRgdId(rgdId.getRgdId());
        qtl.setSpeciesTypeKey(rgdId.getSpeciesTypeKey());
        qtl.setSymbol(data.getSymbol());
        qtl.setName(data.getName());
        qtlDAO.insertQTL(qtl);

        return qtl;
    }

    public void insertXdb(XdbId xdb) throws Exception{
        List<XdbId> xdbs = new ArrayList<>();
        xdbs.add(xdb);
        xdbIdDAO.insertXdbs(xdbs);
    }

    /**
     * return all annotation details for given annotation; used to check if given annotation exists in database;
     * full_annot unique key values must be set in the annotation object
     * @param annot Annotation object with following fields set: REF_RGD_ID, TERM_ACC, RGD_ID, EVIDENCE, WITH_INFO, QUALIFIER, XREF_SOIRCE, TERM and EXP_RGD_ID
     * @return Annotation object with all others field set, including FULL_ANNOT_KEY, or null
     * @throws Exception
     */
    public Annotation getPhenotypeIds(Annotation annot)  throws Exception{

        return annotationDAO.getAnnotation(annot);
    }

    public int updatePhenotypeAnnotLastModifiedDate(int annotKey) throws Exception {

        return annotationDAO.updateLastModified(annotKey);
    }

    public void insertPhenotypeIds(Annotation phenotype) throws Exception{

        annotationDAO.insertAnnotation(phenotype);
        logAnnots.info("INSERT "+phenotype.dump("|"));
    }

    public List<MapData> getMapData(int qtlRgdId) throws Exception {
        return mapDAO.getMapData(qtlRgdId);
    }

    public void updateMapData(MapData md) throws Exception {
        mapDAO.updateMapData(md);
    }

    public void insertMapData(MapData md) throws Exception {
        mapDAO.insertMapData(md);
    }

    public void deleteMapData(MapData md) throws Exception {
        mapDAO.deleteMapData(md.getKey());
    }

    public List<XdbId> getXdbIds(XdbId filter) throws Exception{
        return xdbIdDAO.getXdbIds(filter);

    }
    public Term getTerm( String termAcc) throws Exception{
        return ontologyDAO.getTerm(termAcc);
    }

    /**
     * get chromosome size for given chromosome and map
     * @param mapKey map key
     * @param chromosome chromosome
     * @return chromosome size
     * @throws Exception
     */
    public int getChromosomeSize(int mapKey, String chromosome) throws Exception {

        // we store chromosome sizes in a special map; key is 'CHROMOSOME|mapKey', value is chromosome size
        String key = chromosome+"|"+mapKey;
        Integer chrSize = mapChrSizes.get(key);
        if( chrSize==null ) {
            // load chromosome sizes for given mapKey from database and put the values into map
            for( java.util.Map.Entry<String,Integer> entry: mapDAO.getChromosomeSizes(mapKey).entrySet() ) {
                mapChrSizes.put(entry.getKey()+"|"+mapKey, entry.getValue());
            }
            chrSize = mapChrSizes.get(key);
        }
        return chrSize;
    }
    private final HashMap<String,Integer> mapChrSizes = new HashMap<>(31);


    public void updateLastModifiedDate (int rgdId) throws Exception {
        rgdDAO.updateLastModifiedDate(rgdId);
    }

    public List getQtlReferences(int qtlRgdId) throws Exception {
        return associationDAO.getReferenceAssociations(qtlRgdId);
    }

    public void addQtlReference(int qtlRgdId, int refRgdId) throws Exception {
        associationDAO.insertReferenceeAssociation(qtlRgdId, refRgdId);
    }
}