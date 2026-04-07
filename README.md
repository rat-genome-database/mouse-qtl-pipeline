# mouse-qtl-pipeline

Loads mouse QTL data from MGI into RGD.

## Source files

Downloaded from MGI:
- `MGI_QTLAllele.rpt` — QTL allele data (QTL symbol, name, allele info)
- `MRK_List2.rpt` — marker list (MGI IDs, symbols, types, coordinates)
- `MGI_MRK_Coord.rpt` — genomic coordinates for markers

## Logic

1. **Download** — fetches the three report files from MGI
2. **Parse allele file** — extracts QTL symbols, names, alleles, and PubMed IDs
3. **Parse marker list** — enriches QTL data with MGI IDs and marker types
4. **Parse coordinate file** — adds genomic coordinates (GRCm38, GRCm39)
5. **Load into RGD** — for each QTL:
   - Inserts new QTLs or updates existing ones (matched by MGI ID)
   - Syncs cM and genomic map positions (inserts, updates, deletes as needed)
   - Adjusts QTL size to average if below configured minimum
   - Processes PubMed IDs and creates reference associations
   - Creates MP (Mammalian Phenotype) annotations with evidence code IEA

## Configuration

Configured in `properties/AppConfigure.xml`:
- `minQtlSize` / `avgQtlSize` — size adjustment thresholds
- `genomicMaps` — supported genome builds (GRCm38, GRCm39)
- `evidenceCode`, `createdBy`, `refRgdId`, `dataSrc` — annotation parameters

## Build and run

Requires Java 17. Built with Gradle:
```
./gradlew clean assembleDist
```
