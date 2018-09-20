package edu.mcw.rgd;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: KPulakanti
 * Date: Jun 29, 2010
 * Time: 11:57:57 AM
 */
public class QtlData {

    // this data come from file MGI_QTLAllele.rpt
    String mgiId;
    List<String> pmIdList; // pubmed ids for all qtl alleles
    String QTLsymbol;
    List<String> mpIdList;
    String chrnum; // chromosome from MRK_List1 file
    String cmposition; // CM absolute position  from MRK_List1 file
    String markname; // marker name
    String start;
    String stop;
    String strand;
    String chromosome; // NCBI genome chromosome from MGI_Coordinate
    String name; // qtl name
    String notes;
    String genomeBuild; // such as 'GRCm38'

    // incremented for every single thing that needs to be updated in database
    // like new maps, new pubmed ids, new annotations, ...
    int updateCount = 0;

    @Override
    public String toString() {
        return mgiId+", pmIds="+pmIdList+", QTLsymbol="+QTLsymbol+", mpIds="+mpIdList+"," +
                " chrnumber="+chrnum+",cmposition="+cmposition+",markername="+markname+
                ",start="+start+",stop="+stop+",strand="+strand+",name="+name+"";
               
    }

    public String getPubMedIdsAsString() {

        String ids = "";
        for( String pubMedId: pmIdList ) {
            if( ids.length()==0 )
                ids = "PMID:"+pubMedId;
            else
                ids += "|PMID:"+pubMedId;
        }
        return ids;
    }
    
    public String getMgiId() {
       return mgiId;
   }

   public void setMgiId(String mgiId) {
       this.mgiId = mgiId;
   }

   public List<String> getPmIdList() {
       return pmIdList;
   }

   public void setPmIdList(List<String> pmIdList) {
       this.pmIdList = pmIdList;
   }

   public String getSymbol() {
       return QTLsymbol;
   }

   public void setSymbol(String QTLsymbol) {
       this.QTLsymbol = QTLsymbol;
   }

    public List<String> getMpIdList() {
        return mpIdList;
    }

    public void setMpIdList(List<String> mpIdList) {
        this.mpIdList = mpIdList;
    }
    public String getChrnum() {
       return chrnum;
   }

   public void setChrnum(String chrnum) {
       this.chrnum = chrnum;
   }
    public String getCmposition() {
       return cmposition;
   }

   public void setCmposition(String cmposition) {
       this.cmposition = cmposition;
   }
    public String getMarkname() {
       return markname;
   }

   public void setMarkname(String markname) {
       this.markname = markname;
   }
    public String getStart() {
       return start;
   }

   public void setStart(String start) {
       if( this.start!=null && !this.start.equals(start) ) {
           System.out.println("start pos mutating ");
       }
       this.start = start;
   }

   public String getStop() {
       if( this.start!=null && !this.start.equals(start) ) {
           System.out.println("start pos mutating ");
       }
       return stop;
   }

    public void setStop(String stop) {
        this.stop = stop;
    }

    public String getStrand() {
        return strand;
    }

    public void setStrand(String strand) {
        this.strand = strand;
    }

    public String getChromosome() {
        return chromosome;
    }

    public void setChromosome(String chromosome) {
        this.chromosome = chromosome;
    }

   public String getName() {
       return name;
   }

   public void setName(String name) {
       this.name = name;
   }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public int getUpdateCount() {
        return updateCount;
    }

    public int incrementUpdateCount() {
        return ++updateCount;
    }

    public String getGenomeBuild() {
        return genomeBuild;
    }

    public void setGenomeBuild(String genomeBuild) {
        this.genomeBuild = genomeBuild;
    }
}
