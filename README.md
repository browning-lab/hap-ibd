# hap-ibd

The **hap-ibd** program detects identity-by-descent (IBD) segments and
homozygosity-by-descent (HBD) segments in phased genotype data.  The **hap-ibd**
program can analyze data sets with hundreds of thousands of samples.

If you use **hap-ibd** in a published analysis, please report the program
version printed in the first line of the output **log** file and
please cite the article that describes the **hap-ibd** method:

> Y Zhou, S R Browning, B L Browning. A fast and simple method for detecting
identity by descent segments in large-scale data. bioRxiv 2019.12.12.874685;
doi: https://biorxiv.org/cgi/content/short/2019.12.12.874685v1

Brian Browning

Last updated December 12, 2019

## Installation

You can download the latest executable file,
[hap-ibd.jar](https://faculty.washington.edu/browning/hap-ibd.jar),
with the command:

    wget https://faculty.washington.edu/browning/hap-ibd.jar

or you can download the source files and create the executable file
with the commands:

    git clone https://github.com/browning-lab/hap-ibd.git
    javac -cp hap-ibd/src/ hap-ibd/src/hapibd/HapIbdMain.java
    jar cfe hap-ibd.jar hapibd/HapIbdMain -C hap-ibd/src/ ./
    jar -i hap-ibd.jar

## Running hap-ibd

The **hap-ibd** program requires Java version 1.8 (or a later version). Use of an
earlier Java version will produce an "Unsupported Class Version" error.

The command:

    java -jar hap-ibd.jar

prints a summary of the command line arguments.

To run **hap-ibd**, enter the following command:

    java -Xmx[GB]g -jar hap-ibd.jar [arguments]

where **[GB]** is the maximum number of gigabytes of memory to use, and
**[arguments]** is a space-separated list of parameter values, each expressed as
**parameter=value**.

The shell script
[run.hap-ibd.test](https://raw.githubusercontent.com/browning-lab/hap-ibd/master/test/run.hap-ibd.test)
will run a test **hap-ibd** analysis.

### Required Parameters

The **hap-ibd** program has three required parameters.

* **gt=[file]** where **[file]** is a
[Variant Call Format](https://faculty.washington.edu/browning/intro-to-vcf.html)
(VCF) file.  All VCF records must include a GT FORMAT subfield, all genotypes
must be phased, and there can be no missing alleles. If your data
is unphased, you can phase your data using the
[Beagle](https://faculty.washington.edu/browning/beagle/beagle.html) program.
A VCF record may have multiple ALT alleles. A VCF file with a name ending in
".gz" is assumed to be gzip-compressed.

* **map=[file]** where **[file]** is a
[PLINK format genetic map](http://zzz.bwh.harvard.edu/plink/data.shtml#map)
with cM units. Ibis will use linear interpolation to estimate genetic
positions between map positions. Chromosome identifiers in the genetic map and
input VCF file must match. HapMap genetic maps in cM units are available for
[GRCh36](http://bochet.gcc.biostat.washington.edu/beagle/genetic_maps/plink.GRCh36.map.zip),
[GRCh37](http://bochet.gcc.biostat.washington.edu/beagle/genetic_maps/plink.GRCh37.map.zip), and
[GRCh38](http://bochet.gcc.biostat.washington.edu/beagle/genetic_maps/plink.GRCh38.map.zip).

* **out=[string]** where **[string]** is the output filename prefix.

* **excludesamples=[file]** where [file] is a text file containing samples
(one sample per line) to be excluded from the analysis.

### Optional Parameters

* **min-seed=[number > 0.0]** specifies the minimum cM length of a seed
(identity-by-state) IBS segment that is eligible to be extended
(**default: min-seed=2.0**). See the **max-gap** and **min-extend** parameters
for more information.

* **max-gap=[integer ≥ -1]** specifies the maximum base-pair gap between a
seed segment and another IBS segment in order for the
the seed segment to be extended (**default: max-gap=1000**).
The base-pair gap is the absolute value of the difference between the
VCF POS field of the first and last marker in the non-IBS region between
the two segments. The **max-gap** parameter allows output IBD segments to
include very short non-IBS regions that can result from genotype error,
mutation, and gene conversion.  If **max-gap=-1** no IBS/HBS segments will be
merged. See the **min-seed** and **min-extend** parameters for more information.

* **min-extend=[number ≤ min-seed]** specifies the minimum cM length of an IBS
segment that can extend a seed segment.  The default
value is minimum(1.0, **min-seed**). See the **min-seed** and **max-gap**
parameters  for more details.

* **min-output=[number > 0.0]** specifies the minimum cM
length of output IBD/HBD segments. (**default: min-length=2.0**).
Each output HBD/IBD segment is composed of a seed IBS segment and any
neighboring extension IBS segments. See the **min-seed**, **max-gap**, and
**min-extend** parameters for more details.

* **min-markers=[number ≥ 1]** specifies the minimum number of markers in
each seed and extension IBS segment (**default: min-markers=1**).
An IBS seed segment is required to contain at least **min-markers** markers.
An IBS extension segment is required to contain at last
((**min-seed**/**min_extend**) &times; **min-markers**) markers.
Increasing the **min-markers** parameter can reduce inflation in
the number of output IBD segments in regions with sparse marker coverage.
See the **max-seed** and **min-extend** parameters for more information.

* **min-mac=[integer ≥ 1]** specifies the minimum number of copies of the minor
allele. If a marker has fewer than the minimum number of minor allele carriers,
the marker will be excluded from the analysis (**default: min-mac=2**).
For multi-allelic markers, the minor allele count is the number of copies of
the allele with the second-largest allele frequency.

* **nthreads=[integer ≥ 1]** specifies the number of computational threads to
use. The default **nthreads** parameter is the number of CPU cores.


## Output files
The **hap-ibd** program produces three output files: a **log** file, an
 **ibd** file, and an **hbd** file.

The **log** file (.log) contains a summary of the analysis, which includes
the analysis parameters, the number of markers, the number of samples,
the number of output HBD and IBD segments, and the mean number of HBD and
IBD segments per sample.

The gzip-compressed **ibd** file (.ibd.gz) contains IBD segments shared between
individuals. The gzip-compressed **hbd** file (.hbd.gz) contains HBD
segments within within individuals. Each line of the **ibd** and **hbd** output
files represents one IBD or HBD segment and contains 10 tab-delimited fields:

1. First sample identifier
2. First sample haplotype index (1 or 2)
3. Second sample identifier
4. Second sample haplotype index (1 or 2)
5. Chromosome
6. Base coordinate of first marker in segment
7. Base coordinate of last marker in segment
8. cM length of IBD segment

## License
The **hap-ibd** program is licensed under the Apache License, Version 2.0 (the License).
You may obtain a copy of the License from http://www.apache.org/licenses/LICENSE-2.0

Source files in the net/sf/samtools/ directory are from the Broad Institute
and are licensed under the [MIT License](https://opensource.org/licenses/MIT).
The Broad Institute files are used to perform BGZIP decompression.
