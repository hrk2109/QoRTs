package internalUtils

import net.sf.samtools._

import internalUtils.Reporter._;
import internalUtils.stdUtils._;
import internalUtils.fileUtils._;
import internalUtils.commandLineUI._;
import java.io.File;

import scala.collection.JavaConversions._

object commonSeqUtils {
  
  /******************************************************************************************************
   * Minor Utilities:
   */
  def getAlignmentStart(r : SAMRecord) : Int = {
    r.getAlignmentStart() - 1;
  }
  def getAlignmentEnd(r : SAMRecord) : Int = {
    r.getAlignmentEnd();
  }
  def getTailClipping(r : SAMRecord) : Int = {
    val lastCigarElement = r.getCigar().getCigarElements().last;
    if(lastCigarElement.getOperator().equals(CigarOperator.SOFT_CLIP)){
      return lastCigarElement.getLength();
    } else if(lastCigarElement.getOperator().equals(CigarOperator.HARD_CLIP)){
      val nextToLastCigarElement = r.getCigar().getCigarElements().takeRight(2).head;
      if(nextToLastCigarElement.getOperator().equals(CigarOperator.SOFT_CLIP)){
        return nextToLastCigarElement.getLength();
      } else return 0;
    } else return 0;
  }
  def getLeadClipping(r : SAMRecord) : Int = {
    val firstCigarElement = r.getCigar().getCigarElements().head;
    if(firstCigarElement.getOperator().equals(CigarOperator.SOFT_CLIP)){
      return firstCigarElement.getLength();
    } else if(firstCigarElement.getOperator().equals(CigarOperator.HARD_CLIP)){
      val secondCigarElement = r.getCigar().getCigarElements().tail.head;
      if(secondCigarElement.getOperator().equals(CigarOperator.SOFT_CLIP)){
        return secondCigarElement.getLength();
      } else return 0;
    } else return 0;
  }
  
  //def reverseBaseByte : 
  

  
  val reverseBaseByteArray : Array[Byte] = Array.ofDim[Byte](255);
  reverseBaseByteArray('A'.toInt) = 'T'.toByte;
  reverseBaseByteArray('T'.toInt) = 'A'.toByte;
  reverseBaseByteArray('G'.toInt) = 'C'.toByte;
  reverseBaseByteArray('C'.toInt) = 'G'.toByte;
  reverseBaseByteArray('N'.toInt) = 'N'.toByte;
  reverseBaseByteArray('H'.toInt) = 'H'.toByte;
  
  def reverseBaseByte(b : Byte) : Byte = reverseBaseByteArray(b);
  def reverseBaseChar(c : Char) : Char = reverseBaseByteArray(c).toChar;
  
  val reverseBaseByteMap : scala.collection.mutable.Map[Byte,Byte] = scala.collection.mutable.Map[Byte,Byte]();
  reverseBaseByteMap('A'.toByte) = 'T'.toByte;
  reverseBaseByteMap('T'.toByte) = 'A'.toByte;
  reverseBaseByteMap('G'.toByte) = 'C'.toByte;
  reverseBaseByteMap('C'.toByte) = 'G'.toByte;
  reverseBaseByteMap('N'.toByte) = 'N'.toByte;
  reverseBaseByteMap('H'.toByte) = 'H'.toByte;
  
  val reverseBaseCharMap : scala.collection.mutable.Map[Char,Char] = scala.collection.mutable.Map[Char,Char]();
  reverseBaseCharMap('A') = 'T';
  reverseBaseCharMap('T') = 'A';
  reverseBaseCharMap('G') = 'C';
  reverseBaseCharMap('C') = 'G';
  reverseBaseCharMap('N') = 'N';
  reverseBaseCharMap('H') = 'H';
  
  /******************************************************************************************************
   * Holder and Helper Classes:
   ******************************************************************************************************/
  

  /******************************************************************************************************
   *  Genomic Intervals:
   */    
  case class GenomicInterval(chromName : String, strand : Char, start : Int, end : Int){
    def strandStranded(isStranded : Boolean) : Char = {
      if(isStranded) return strand;
      else return '.';
    }
    
    def unstranded : GenomicInterval = new GenomicInterval(chromName,'.',start,end); 
    def usingStrandedness(isStranded : Boolean) : GenomicInterval = {
      if(isStranded) return this;
      else this.unstranded;
    }
    def switchStrand : GenomicInterval = {
      if(strand == '+') return new GenomicInterval(chromName, '-', start, end);
      if(strand == '-') return new GenomicInterval(chromName, '+', start, end);
      return this;
    }
    
    def overlaps(that : GenomicInterval) : Boolean = {
      return (this.chromName == that.chromName) && (this.strand == that.strand) && (this.start < that.end) && (that.start < this.end);
    }
    def overlaps(thatStart : Int, thatEnd : Int) : Boolean = {
      return (this.start < thatEnd) && (thatStart < this.end);
    }
  }
  trait GIVOrdering extends Ordering[GenomicInterval] {
    def compare(a : GenomicInterval, b : GenomicInterval) = implicitly[Ordering[Tuple4[String,Int,Int,Char]]].compare((a.chromName, a.start, a.end, a.strand) , (b.chromName,b.start, b.end, b.strand))
  }
  implicit object GenomicInterval extends GIVOrdering;

  /******************************************************************************************************
   * Cigar Helpers
   */
  def getCigarSeq(r : SAMRecord, reverse : Boolean) : Seq[CigarElement] = {
    if(reverse){
      r.getCigar().getCigarElements().toVector.reverse;
    } else {
      r.getCigar().getCigarElements().toVector;
    }
  }
  
  /*def getHardClipLengths(r : SAMRecord) : (Int,Int) = {
    val cigarElements = r.getCigar.getCigarElements();
    val (leadElement, tailElement) = if(r.getReadNegativeStrandFlag()){
      (cigarElements.last, cigarElements.head);
    } else {
      (cigarElements.head, cigarElements.last);
    }
    
    val leadClip = if(leadElement.getOperator() == CigarOperator.HARD_CLIP) leadElement.getLength() else 0;
    val tailClip = if(tailElement.getOperator() == CigarOperator.HARD_CLIP) tailElement.getLength() else 0;
    return ((leadClip,tailClip));
  }*/
  
  def getUnclippedReadLength(r : SAMRecord) : Int = {
      val baseLength = r.getReadLength();
      if(! r.getReadUnmappedFlag()){
        val cigar = r.getCigar().getCigarElements();
        val leftClip  = if(cigar.head.getOperator() == CigarOperator.HARD_CLIP) cigar.head.getLength() else 0;
        val rightClip = if(cigar.last.getOperator() == CigarOperator.HARD_CLIP) cigar.last.getLength() else 0;
        baseLength + leftClip + rightClip;
      } else {
        baseLength;
      }
  }
  
  //HardClip, SoftClip
  def getLeadClipLengths(cigarSeq : Seq[CigarElement]) : (Int,Int) = {
    if(cigarSeq.head.getOperator() == CigarOperator.HARD_CLIP){
      if(cigarSeq(1).getOperator() == CigarOperator.SOFT_CLIP){
        (cigarSeq.head.getLength(), cigarSeq(1).getLength());
      } else {
        (cigarSeq.head.getLength(), 0);
      }
    } else {
      if(cigarSeq.head.getOperator() == CigarOperator.SOFT_CLIP){
        (0, cigarSeq.head.getLength());
      } else {
        (0,0);
      }
    }
  }
  def getTailClipLengths(cigarSeq : Seq[CigarElement]) : (Int,Int) = {
    if(cigarSeq.last.getOperator() == CigarOperator.HARD_CLIP){
      if(cigarSeq(cigarSeq.length - 2).getOperator() == CigarOperator.SOFT_CLIP){
        (cigarSeq.last.getLength(), cigarSeq(cigarSeq.length - 2).getLength());
      } else {
        (cigarSeq.last.getLength(), 0);
      }
    } else {
      if(cigarSeq.last.getOperator() == CigarOperator.SOFT_CLIP){
        (0, cigarSeq.last.getLength());
      } else {
        (0,0);
      }
    }
  }
  // ((leadHardClip, leadSoftClip),(tailHardClip, tailSoftClip))
  def getAllClipLengthsSimple(r : SAMRecord) : ((Int,Int), (Int, Int)) = {
    val cigarSeq = getCigarSeq(r, r.getReadNegativeStrandFlag());
    return (getLeadClipLengths(cigarSeq), getTailClipLengths(cigarSeq));
  }
  
  def getAllClipLengthsWithAppending(r : SAMRecord, readLength : Int, assumeTailClipping : Boolean) : ((Int,Int), (Int, Int)) = {
    val cigarSeq = getCigarSeq(r, r.getReadNegativeStrandFlag());
    val (leadHardClip, leadSoftClip) = getLeadClipLengths(cigarSeq);
    val (tailHardClip, tailSoftClip) = getTailClipLengths(cigarSeq);
    
    val observedLength = r.getReadBases().length;
    val missingLength = readLength - observedLength - leadHardClip - tailHardClip;
    
    if(assumeTailClipping){
      return ((leadHardClip,leadSoftClip),(tailHardClip + missingLength,tailSoftClip))
    } else {
      return ((leadHardClip + missingLength,leadSoftClip),(tailHardClip ,tailSoftClip))
    }
  }
  
  
  def getClipLengths(r : SAMRecord) : (Int, Int) = {
    val cigarSeq = getCigarSeq(r, r.getReadNegativeStrandFlag());
    
    val leadClipLen = if(cigarSeq.head.getOperator() == CigarOperator.SOFT_CLIP) { cigarSeq.head.getLength() } else 0;
    val tailClipLen = if(cigarSeq.last.getOperator() == CigarOperator.SOFT_CLIP) { cigarSeq.last.getLength() } else 0;
    
    return (leadClipLen, tailClipLen);
  }
  
  def getBasePairs(r : SAMRecord) : Seq[Byte] = {
    if(r.getReadNegativeStrandFlag()){
      return r.getReadBases().toVector.reverse;
    } else {
      return r.getReadBases().toVector;
    }
  }
  
  def getBasePairsWithH(r : SAMRecord, readLength : Int, assumeTailClipping : Boolean) : Seq[Byte] = {
    if(r.getReadNegativeStrandFlag()){
      val readBases = r.getReadBases().toVector.reverse;
      val cigarElements = r.getCigar().getCigarElements()
      val leadCigarElement = cigarElements.last;
      val tailCigarElement = cigarElements.head;
      
      val leadSeq = if(leadCigarElement.getOperator() == CigarOperator.HARD_CLIP) repToVector[Byte]('H'.toByte,leadCigarElement.getLength()) else Vector[Byte]();
      val tailSeq = if(tailCigarElement.getOperator() == CigarOperator.HARD_CLIP) repToVector[Byte]('H'.toByte,tailCigarElement.getLength()) else Vector[Byte]();
      val out = leadSeq ++ readBases ++ tailSeq
      if(out.length < readLength){
        if(assumeTailClipping){
          return out ++ repToVector[Byte]('H'.toByte,readLength - out.length);
        } else {
          return repToVector[Byte]('H'.toByte,readLength - out.length) ++ out;
        }
      } else {
        return out;
      }
    } else {
      val readBases = r.getReadBases().toVector;
      val cigarElements = r.getCigar().getCigarElements()
      val leadCigarElement = cigarElements.head;
      val tailCigarElement = cigarElements.last;
      
      val leadSeq = if(leadCigarElement.getOperator() == CigarOperator.HARD_CLIP) repToVector[Byte]('H'.toByte,leadCigarElement.getLength()) else Vector[Byte]();
      val tailSeq = if(tailCigarElement.getOperator() == CigarOperator.HARD_CLIP) repToVector[Byte]('H'.toByte,tailCigarElement.getLength()) else Vector[Byte]();
      val out = leadSeq ++ readBases ++ tailSeq
      
      if(out.length < readLength){
        if(assumeTailClipping){
          return out ++ repToVector[Byte]('H'.toByte,readLength - out.length);
        } else {
          return repToVector[Byte]('H'.toByte,readLength - out.length) ++ out;
        }
      } else {
        return out;
      }
    }
  }
  
  /******************************************************************************************************
   * CigarOp Holder:
   */
  object CigarHolder{
    def apply(r : SAMRecord, stranded : Boolean, fr_secondStrand : Boolean) : CigarHolder = new CigarHolder(r.getCigar(), r.getAlignmentStart() - 1, r.getReferenceName(), getStrand(r,stranded,fr_secondStrand));
  }
  
  case class CigarHolder(cigar : Cigar, alignmentStart : Int, chromName : String, strand : Char){
    lazy val cigOps : Stream[CigOp] = cigar.getCigarElements.toStream.scanLeft[(Int,Int,CigOp),Stream[(Int,Int,CigOp)]]( (alignmentStart,0,null) )( (startTriplet , ce) => {
      val (ref_start, read_start, prev) = startTriplet;
      val newOp = CigOp(ce, ref_start, read_start, chromName, strand);
      ((newOp.ref_end, newOp.read_end, newOp))
    }).tail.map(_._3);
  }
  
  object CigOp{ 
    def apply(ce : CigarElement, ref_start : Int, read_start : Int, chromName : String, strand : Char): CigOp = {
      new CigOp(ce.getOperator(), ce.getLength(), ref_start, read_start, chromName, strand);
    }
  }
  case class CigOp(op : CigarOperator, length : Int, ref_start : Int, read_start : Int, chromName : String, strand : Char){
    lazy val read_end : Int = if(op.consumesReadBases()) read_start + length else read_start;
    lazy val ref_end : Int = if(op.consumesReferenceBases()) ref_start + length else ref_start;
    lazy val ref_iv : GenomicInterval = new GenomicInterval(chromName, strand, ref_start, ref_end);
    lazy val read_iv : (Int,Int) = (read_start,read_end);
  }
  //cigop variables: op, length, ref_start, ref_end, read_start, read_end, chromName, strand, ref_iv, read_iv;

  /******************************************************************************************************
   * Alignment Block manipulation:
   */
  def getBlockReferenceSpan(b : AlignmentBlock) : (Int,Int) = {
    val start = b.getReferenceStart - 1;
    (start, b.getLength() + start);
  }
  def getBlockReferenceStart(b : AlignmentBlock) : Int = {
    b.getReferenceStart - 1;
  }
  def getBlockReferenceEnd(b : AlignmentBlock) : Int = {
    b.getReferenceStart - 1 + b.getLength();
  }
  def getBlockReadStart(b : AlignmentBlock) : Int = {
    b.getReadStart - 1;
  }
  def getBlockReadEnd(b : AlignmentBlock) : Int = {
    b.getReadStart - 1 + b.getLength();
  }
  
  /******************************************************************************************************
   * SAM File Iterators and readers:
   ******************************************************************************************************/
  

  
  /*def initSamRecordIterator_DEPRECIATED(infile : String, peekCount : Int = 1000) : (Int, Iterator[SAMRecord]) = {
    val preOpen : SAMFileReader = new SAMFileReader(new File(infile));
    val iter : Iterator[SAMRecord] = preOpen.iterator();
    var peekRecords = Seq[SAMRecord]();
    
    for(i <- 0 until peekCount){
      val next = iter.next;
      try {
        peekRecords = peekRecords :+ next;
      } catch {
        case e : Exception => throw e;
      }
    }
    //val peekRecords : Seq[SAMRecord] = iter.take(peekCount).toSeq;
    val readLength = peekRecords.maxBy( _.getReadLength ).getReadLength;

    return ((readLength, peekRecords.iterator ++ iter));
  }*/
  
  /*def peekSamRecordIterator(infile : String, peekCount : Int = 1000) : SamFileAttributes = {
    val reader : SAMFileReader = if(infile == "-"){
      new SAMFileReader(System.in);
    } else {
      new SAMFileReader(new File(infile));
    }
    val iter : Iterator[SAMRecord] = reader.iterator();
    var peekRecords = Seq[SAMRecord]();
     
    for(i <- 0 until peekCount){
      if(iter.hasNext){
        val next = iter.next;
        try {
          peekRecords = peekRecords :+ next;
        } catch {
          case e : Exception => throw e;
        }
      } else { 
        //do nothing!
      }
    }
     
    val isSortedByName : Boolean = (Iterator.from(1,2).takeWhile(_ < peekRecords.size).map(peekRecords(_))).zip( (Iterator.from(0,2).takeWhile(_ < peekRecords.size).map(peekRecords(_))) ).forall( (r12) => r12._1.getReadName == r12._2.getReadName );
    val isDefinitelyPairedEnd : Boolean = peekRecords.exists( r => ( peekRecords.count(_.getReadName() == r.getReadName()) > 1 ) );
    val readLength = peekRecords.maxBy( _.getReadLength ).getReadLength;
    val isSortedByPosition : Boolean = (peekRecords).zip(peekRecords.tail).forall( r12 => {
        val iv1 = internalUtils.genomicUtils.getGenomicIntervalsFromRead(r12._1, true, false).next;
        val iv2 = internalUtils.genomicUtils.getGenomicIntervalsFromRead(r12._2, true, false).next;
        GenomicInterval.compare(iv1,iv2) > 0;
    });
    val minReadLength : Int = peekRecords.minBy( _.getReadLength ).getReadLength;

    reader.close();
    return SamFileAttributes(readLength, isSortedByName, isSortedByPosition, isDefinitelyPairedEnd, minReadLength);
  }*/
  
  case class SamFileAttributes(readLength : Int, 
                               isSortedByName : Boolean, 
                               isSortedByPosition : Boolean, 
                               isDefinitelyPairedEnd : Boolean, 
                               allReadsMarkedPaired : Boolean,
                               allReadsMarkedSingle : Boolean,
                               mixedSingleAndPaired : Boolean,
                               minReadLength : Int);
  
  def initSamRecordIterator(infile : String, peekCount : Int = 1000, bufferSize : Int = 10000) : (SamFileAttributes, Iterator[SAMRecord]) = {
    val reader : SAMFileReader = if(infile == "-"){
      new SAMFileReader(System.in);
    } else {
      new SAMFileReader(new File(infile));
    }
    val iter : Iterator[SAMRecord] = reader.iterator();
    var peekRecords = Seq[SAMRecord]();
     
    for(i <- 0 until peekCount){
      if(iter.hasNext){
        val next = iter.next;
        try {
          peekRecords = peekRecords :+ next;
        } catch {
          case e : Exception => throw e;
        }
      } else { 
        //do nothing!
      }
    }
     
    //val isSortedByName : Boolean = (Iterator.from(1,2).takeWhile(_ < peekRecords.size).map(peekRecords(_))).zip( (Iterator.from(0,2).takeWhile(_ < peekRecords.size).map(peekRecords(_))) ).forall( (r12) => r12._1.getReadName == r12._2.getReadName );
    val isSortedByName : Boolean = seqIsSortedBy(peekRecords, (r1 : SAMRecord, r2 : SAMRecord) => {
        val n1 = r1.getReadName();
        val n2 = r2.getReadName();
        n1.compareTo(n2) <= 0;
    })
    
    val isDefinitelyPairedEnd : Boolean = peekRecords.exists( r => ( peekRecords.count(_.getReadName() == r.getReadName()) > 1 ) );
    //val readLength = peekRecords.maxBy( _.getReadLength ).getReadLength;
    
    val allReadLengths = peekRecords.map( (r : SAMRecord) => getUnclippedReadLength(r));
    
    
    //val allReadLengths = peekRecords.map( (r : SAMRecord) => {  
    //  val baseLength = r.getReadLength();
    //  if(! r.getReadUnmappedFlag()){
    //    val cigar = r.getCigar().getCigarElements();
    //    val leftClip  = if(cigar.head.getOperator() == CigarOperator.HARD_CLIP) cigar.head.getLength() else 0;
    //    val rightClip = if(cigar.last.getOperator() == CigarOperator.HARD_CLIP) cigar.last.getLength() else 0;
    //    baseLength + leftClip + rightClip;
    //  } else {
    //    baseLength;
    //  }
    //});
    val readLength = allReadLengths.max;
    val minReadLength = allReadLengths.min;
    
    val isSortedByPosition : Boolean = seqIsSortedBy(peekRecords, (r1 : SAMRecord, r2 : SAMRecord) => {
        val start1 = r1.getAlignmentStart();
        val start2 = r2.getAlignmentStart();
        val chrom1 = r1.getReferenceName();
        val chrom2 = r2.getReferenceName();
        if(chrom1 == chrom2){
          start1 <= start2;
        } else {
          true;
        }
    })
    //val isSortedByPosition : Boolean = (peekRecords).zip(peekRecords.tail).forall( r12 => {
    //    val (r1 : SAMRecord,r2 : SAMRecord) = r12;
    //    
    //    val iv1 = internalUtils.genomicUtils.getGenomicIntervalsFromRead(r1, true, false).next;
    //    val iv2 = internalUtils.genomicUtils.getGenomicIntervalsFromRead(r2, true, false).next;
    //    GenomicInterval.compare(iv1,iv2) > 0;
    //});
    //val minReadLength : Int = peekRecords.minBy( _.getReadLength ).getReadLength;
    
    val outIter = if(bufferSize == 0){ 
      peekRecords.iterator ++ iter
    } else {
      peekRecords.iterator ++ bufferIterator(iter, bufferSize);
    }
    
    
    val allReadsMarkedPaired : Boolean = peekRecords.forall( _.getReadPairedFlag);
    val allReadsMarkedSingle : Boolean = peekRecords.forall(! _.getReadPairedFlag);
    val mixedSingleAndPaired : Boolean = ! (allReadsMarkedPaired | allReadsMarkedSingle);

    return ((SamFileAttributes(readLength, isSortedByName, isSortedByPosition, isDefinitelyPairedEnd, allReadsMarkedPaired, allReadsMarkedSingle, mixedSingleAndPaired, minReadLength), outIter));
  }
  
  //def samRecordPairIterator_allPossibleSituations(iter : Iterator[SAMRecord], attr : SamFileAttributes, verbose : Boolean = true, testCutoff : Int = Int.MaxValue) : Iterator[(SAMRecord,SAMRecord)] = {
    
  //}
  
  def samRecordPairIterator_unsorted(iter : Iterator[SAMRecord], verbose : Boolean = true, testCutoff : Int = -1, ignoreSecondary : Boolean = true) : Iterator[(SAMRecord,SAMRecord)] = {

    if(ignoreSecondary){
       presetProgressReporters.wrapIterator_readPairs(getSRPairIterUnsorted(iter.filter((read : SAMRecord) => {
           (! read.getNotPrimaryAlignmentFlag()) & (! read.getMateUnmappedFlag()) & (! read.getReadUnmappedFlag())
         })), verbose, testCutoff);
    } else {
      error("FATAL ERROR: Using non-primary read mappings is not currently implemented!");
      return null;
    }
  }
  def samRecordPairIterator_withMulti(iter : Iterator[SAMRecord], verbose : Boolean = true, testCutoff : Int = -1, ignoreSecondary : Boolean = true) : Iterator[(SAMRecord,SAMRecord)] = {
    if(ignoreSecondary){
      presetProgressReporters.wrapIterator_readPairs(getSRPairIter(iter.filter((read : SAMRecord) => {
        (! read.getNotPrimaryAlignmentFlag()) & (! read.getMateUnmappedFlag()) & (! read.getReadUnmappedFlag())
      })), verbose, testCutoff);
    } else {
      error("FATAL ERROR: Using non-primary read mappings is not currently implemented!");
      return null;
    }
  }
  def samRecordPairIterator_withMulti_singleEnd(iter : Iterator[SAMRecord], verbose : Boolean = true, testCutoff : Int = -1, ignoreSecondary : Boolean = true) : Iterator[(SAMRecord,SAMRecord)] = {
    if(ignoreSecondary){
      presetProgressReporters.wrapIterator_readPairs(getSRPairIter_singleEnd(iter.filter((read : SAMRecord) =>{
        (! read.getNotPrimaryAlignmentFlag()) & (! read.getReadUnmappedFlag())
      })), verbose, testCutoff);
    } else {
      error("FATAL ERROR: Using non-primary read mappings is not currently implemented!");
      return null;
    }
  }
  
  
  // Feature Removed!
  // Reason: Cannot guarantee reasonable memory footprint.
  /*def samRecordPairIterator_unsorted(iter : Iterator[SAMRecord], verbose : Boolean = true, testCutoff : Int = Int.MaxValue, ignoreSecondary : Boolean = true) : Iterator[(SAMRecord,SAMRecord)] = {
    if(ignoreSecondary){
      val pairIter : Iterator[(SAMRecord,SAMRecord)] = new  Iterator[(SAMRecord,SAMRecord)] {
        var pairBuffer : Map[String,SAMRecord] = Map[String,SAMRecord]();
        
        private def prepNext : (SAMRecord,SAMRecord) = {
          while(iter.hasNext){
            val rB = iter.next;
            if(! rB.getNotPrimaryAlignmentFlag){
              val rn = rB.getReadName();
              if(pairBuffer.containsKey(rn)){
                val rA = pairBuffer(rn);
                pairBuffer = pairBuffer - rn;
                if(rA.getFirstOfPairFlag) return((rA,rB));
                else return((rB,rA));
              } else {
                pairBuffer = pairBuffer.updated(rn,rB);
              }
            } //else do nothing.
          }
          if(! pairBuffer.isEmpty){
            error("Loose reads left over! No pairs found for: " + pairBuffer.size + " reads, including: \n" + pairBuffer.head._2.getReadString() );
          }
          return(null);
        }
        var nextBuffer : (SAMRecord,SAMRecord) = prepNext;
        
        def hasNext : Boolean = (nextBuffer != null)
        
        def next : (SAMRecord,SAMRecord) = {
          val n = prepNext;
          nextBuffer = prepNext;
          return n;
        }
      }
      return presetProgressReporters.wrapIterator_readPairs(pairIter, verbose, testCutoff);
    } else {
      error("FATAL ERROR: Using non-primary read mappings is not currently implemented!");
      return null;
    }
  }*/
  
  private def getSRPairIter_singleEnd(iter : Iterator[SAMRecord]) : Iterator[(SAMRecord,SAMRecord)] = {
    return iter.map( (next : SAMRecord) => (next,next) );
  }
  
  private def getSRPairIter(iter : Iterator[SAMRecord]) : Iterator[(SAMRecord,SAMRecord)] = {
    return new  Iterator[(SAMRecord,SAMRecord)] {
      def hasNext : Boolean = iter.hasNext;
      def next : (SAMRecord,SAMRecord) = {
        val rA = iter.next;
        val rB = iter.next;
        if(rA.getReadName != rB.getReadName){
          error("FATAL ERROR: SAMRecord is improperly paired! Is the file sorted by name?");
        }
        if(rA.getFirstOfPairFlag) return( (rA,rB) );
        else return( (rB,rA) );
      }
    }
  }
  
  private def getSRPairIterUnsorted(iter : Iterator[SAMRecord]) : Iterator[(SAMRecord,SAMRecord)] = {
    val initialPairContainerWarningSize = 50000;
    val warningSizeMultiplier = 2;
    
    return new Iterator[(SAMRecord,SAMRecord)] {
      val pairContainer = scala.collection.mutable.AnyRefMap[String,SAMRecord]();
      //NOTE: The above container type should never contain more than 500 million reads. If this is a problem, then something is terribly wrong.
      var pairContainerWarningSize = initialPairContainerWarningSize;
      
      def hasNext : Boolean = iter.hasNext;
      def next : (SAMRecord,SAMRecord) = {
        var curr = iter.next;
        
        while((! pairContainer.contains(curr.getReadName())) && iter.hasNext) {
          pairContainer(curr.getReadName()) = curr;
          curr = iter.next;
        }
        
        if(! pairContainer.contains(curr.getReadName()) ){
          reportln("WARNING WARNING WARNING: Reached end of bam file, there are "+(pairContainer.size+1)+" orphaned reads, which are marked as having a mapped pair, but no corresponding pair is found in the bam file. \n(Example Orphaned Read Name: "+curr.getReadName()+")","warn");
        }
        if(pairContainerWarningSize < pairContainer.size){
          reportln("NOTE: Unmatched Read Buffer Size > "+pairContainerWarningSize+" [Mem usage:"+MemoryUtil.memInfo+"]","note");
          if(pairContainerWarningSize == initialPairContainerWarningSize){
            reportln("    (This is generally not a problem, but if this increases further then OutOfMemoryExceptions\n"+
                     "    may occur.\n"+
                     "    If memory errors do occur, either increase memory allocation or sort the bam-file by name\n"+
                     "    and rerun without the '--coordSorted' option.\n"+
                     "    This might also indicate that your dataset contains an unusually large number of\n"+
                     "    chimeric read-pairs. Or it could occur simply due to the presence of genomic\n"+
                     "    loci with extremly high coverage.)", "note");
          }
          pairContainerWarningSize = pairContainerWarningSize * warningSizeMultiplier;
        }
        
        val rB = pairContainer.remove(curr.getReadName()).get;
        if(curr.getFirstOfPairFlag()) return((curr, rB));
        else return((rB, curr));
      }
    }
  }
  
  
  def samRecordPairIterator(iter : Iterator[SAMRecord], verbose : Boolean = true, testCutoff : Int = -1) : Iterator[(SAMRecord,SAMRecord)] = {
    presetProgressReporters.wrapIterator_readPairs(getSRPairIter(iter), verbose, testCutoff);
  }
  
  def getStrand(r : SAMRecord, stranded : Boolean, fr_secondStrand : Boolean) : Char = {
    if(! stranded){
      return '.'
    } else if(! r.getReadPairedFlag()){
      if(fr_secondStrand){
        return if(r.getReadNegativeStrandFlag()) '-' else '+';
      } else {
        return if(r.getReadNegativeStrandFlag()) '+' else '-';
      }
    } else if(fr_secondStrand != r.getFirstOfPairFlag) {
      return if(r.getReadNegativeStrandFlag()) '+' else '-';
    } else {
      return if(r.getReadNegativeStrandFlag()) '-' else '+';
    }
  }
  
  def getStrand(samRecord : SAMRecord) : Char = {
    if(! samRecord.getReadPairedFlag()){
      return if(samRecord.getReadNegativeStrandFlag()) '-' else '+';
    } else if(samRecord.getFirstOfPairFlag()){
      if(samRecord.getReadNegativeStrandFlag) '-' else '+';
    } else {
      if(samRecord.getMateNegativeStrandFlag) '-' else '+';
    }
  }
  
  def isFirstRead(samRecord : SAMRecord) : Boolean = {
    if(! samRecord.getReadPairedFlag()){
      true;
    } else samRecord.getFirstOfPairFlag();
  }
  
  /*************************************************************************************************************************
   * Methods for standard read filtering:
   */
  
  final val CODA_READ_OK = -1;
  final val CODA_MATE_UNMAPPED = 0;
  final val CODA_NOT_PROPER_PAIR = 1;
  final val CODA_READ_FAILS_VENDOR_QC = 2;
  final val CODA_NOT_PRIMARY_ALIGNMENT = 3;
  final val CODA_IS_NOT_VALID = 4;
  final val CODA_CHROMS_MISMATCH = 5;
  final val CODA_PAIR_STRANDS_MISMATCH = 6;
  final val CODA_READ_PAIR_OK = 7;
  final val CODA_READ_ON_IGNORED_CHROMOSOME = 8;
  final val CODA_NOT_UNIQUE_ALIGNMENT = 9;
  
  final val CODA_TOTAL_READ_PAIRS = 10;
  final val CODA_NOT_MARKED_RG = 11;
  
  final val CODA_CODA_LENGTH = 12;
  final val CODA_DEFAULT_OPTIONS : Seq[Boolean] = repToSeq(true,CODA_CODA_LENGTH - 1).toVector ++ Vector(false);
  
  final val CODA_SINGLE_END_OFF_OPTIONS : Seq[Int] = Seq(CODA_MATE_UNMAPPED, CODA_NOT_PROPER_PAIR, CODA_CHROMS_MISMATCH, CODA_PAIR_STRANDS_MISMATCH);
  
  def getNewCauseOfDropArray() : Array[Int] = {
    return Array.ofDim[Int](CODA_CODA_LENGTH);
  }
  
  def causeOfDropArrayToString(causeOfDropArray : Array[Int], dropOptions : Array[Boolean]) : String = {
    //var out = "";
    val sb = new StringBuilder("");
    sb.append("READ_PAIR_OK                   "  + (if(dropOptions(CODA_READ_PAIR_OK)) causeOfDropArray(CODA_READ_PAIR_OK) else "-1"  ) + "\n");
    sb.append("TOTAL_READ_PAIRS               "  + causeOfDropArray(CODA_TOTAL_READ_PAIRS) + "\n");
    //sb.append("DROPPED_UNALIGNED              "  + (if(dropOptions(CODA_MATE_UNMAPPED)) causeOfDropArray(CODA_MATE_UNMAPPED) else "-1"  ) + "\n");
    sb.append("DROPPED_NOT_PROPER_PAIR        "  + (if(dropOptions(CODA_NOT_PROPER_PAIR)) causeOfDropArray(CODA_NOT_PROPER_PAIR) else "-1"  ) + "\n");
    sb.append("DROPPED_READ_FAILS_VENDOR_QC   "  + (if(dropOptions(CODA_READ_FAILS_VENDOR_QC)) causeOfDropArray(CODA_READ_FAILS_VENDOR_QC) else "-1"  ) + "\n");
    ////sb.append("DROPPED_NOT_PRIMARY_ALIGNMENT  "  + (if(dropOptions(CODA_NOT_PRIMARY_ALIGNMENT)) causeOfDropArray(CODA_NOT_PRIMARY_ALIGNMENT) else "-1"  ) + "\n");
    sb.append("DROPPED_MARKED_NOT_VALID       "  + (if(dropOptions(CODA_IS_NOT_VALID)) causeOfDropArray(CODA_IS_NOT_VALID) else "-1"  ) + "\n");
    sb.append("DROPPED_CHROMS_MISMATCH        "  + (if(dropOptions(CODA_CHROMS_MISMATCH)) causeOfDropArray(CODA_CHROMS_MISMATCH) else "-1"  ) + "\n");
    sb.append("DROPPED_PAIR_STRANDS_MISMATCH  "  + (if(dropOptions(CODA_PAIR_STRANDS_MISMATCH)) causeOfDropArray(CODA_PAIR_STRANDS_MISMATCH) else "-1"  ) + "\n");
    sb.append("DROPPED_IGNORED_CHROMOSOME     "  + (if(dropOptions(CODA_READ_ON_IGNORED_CHROMOSOME)) causeOfDropArray(CODA_READ_ON_IGNORED_CHROMOSOME) else "-1"  ) + "\n");
    sb.append("DROPPED_NOT_UNIQUE_ALIGNMENT   "  + (if(dropOptions(CODA_NOT_UNIQUE_ALIGNMENT)) causeOfDropArray(CODA_NOT_UNIQUE_ALIGNMENT) else "-1"  ) + "\n");
    return sb.toString;
  }
  def causeOfDropArrayToStringTabbed(causeOfDropArray : Array[Int], dropOptions : Array[Boolean]) : String = {
    //var out = "";
    val sb = new StringBuilder("");
    sb.append("READ_PAIR_OK	"  + (if(dropOptions(CODA_READ_PAIR_OK)) causeOfDropArray(CODA_READ_PAIR_OK) else "-1"  ) + "\n");
    sb.append("TOTAL_READ_PAIRS	"  + causeOfDropArray(CODA_TOTAL_READ_PAIRS) + "\n");
    //sb.append("DROPPED_UNALIGNED	"  + (if(dropOptions(CODA_MATE_UNMAPPED)) causeOfDropArray(CODA_MATE_UNMAPPED) else "-1"  ) + "\n");
    sb.append("DROPPED_NOT_PROPER_PAIR	"  + (if(dropOptions(CODA_NOT_PROPER_PAIR)) causeOfDropArray(CODA_NOT_PROPER_PAIR) else "-1"  ) + "\n");
    sb.append("DROPPED_READ_FAILS_VENDOR_QC	"  + (if(dropOptions(CODA_READ_FAILS_VENDOR_QC)) causeOfDropArray(CODA_READ_FAILS_VENDOR_QC) else "-1"  ) + "\n");
    ////sb.append("DROPPED_NOT_PRIMARY_ALIGNMENT	"  + (if(dropOptions(CODA_NOT_PRIMARY_ALIGNMENT)) causeOfDropArray(CODA_NOT_PRIMARY_ALIGNMENT) else "-1"  ) + "\n");
    sb.append("DROPPED_MARKED_NOT_VALID	"  + (if(dropOptions(CODA_IS_NOT_VALID)) causeOfDropArray(CODA_IS_NOT_VALID) else "-1"  ) + "\n");
    sb.append("DROPPED_CHROMS_MISMATCH	"  + (if(dropOptions(CODA_CHROMS_MISMATCH)) causeOfDropArray(CODA_CHROMS_MISMATCH) else "-1"  ) + "\n");
    sb.append("DROPPED_PAIR_STRANDS_MISMATCH	"  + (if(dropOptions(CODA_PAIR_STRANDS_MISMATCH)) causeOfDropArray(CODA_PAIR_STRANDS_MISMATCH) else "-1"  ) + "\n");
    sb.append("DROPPED_IGNORED_CHROMOSOME	"  + (if(dropOptions(CODA_READ_ON_IGNORED_CHROMOSOME)) causeOfDropArray(CODA_READ_ON_IGNORED_CHROMOSOME) else "-1"  ) + "\n");
    sb.append("DROPPED_NOT_UNIQUE_ALIGNMENT	"  + (if(dropOptions(CODA_NOT_UNIQUE_ALIGNMENT)) causeOfDropArray(CODA_NOT_UNIQUE_ALIGNMENT) else "-1"  ) + "\n");
    return sb.toString;
  }
   
  def useReadPair(r1 : SAMRecord, r2 : SAMRecord, causeOfDropArray : Array[Int], dropOptions : Seq[Boolean] = CODA_DEFAULT_OPTIONS, dropChrom : Set[String], readGroup : Option[String], minMAPQ : Int = 255) : Boolean = {
    
    causeOfDropArray(CODA_TOTAL_READ_PAIRS) += 1;
    
    if(dropOptions(CODA_NOT_MARKED_RG)){
      if(r1.getReadGroup().getReadGroupId != readGroup.get){
        causeOfDropArray(CODA_NOT_MARKED_RG) += 1;
        return false;
      }
    }
    
    
    val r1c = useRead_code(r1, dropOptions, minMAPQ);
    val r2c = useRead_code(r2, dropOptions, minMAPQ);
    
    if(r1c != CODA_READ_OK){
      causeOfDropArray(r1c) += 1;
      return false;
    } else if(r2c != CODA_READ_OK){
      causeOfDropArray(r2c) += 1;
      return false;
    } else {
      if(dropOptions(CODA_CHROMS_MISMATCH) && r1.getReferenceName() != r2.getReferenceName()){
        causeOfDropArray(CODA_CHROMS_MISMATCH) += 1;
        return false;
      } else if(dropOptions(CODA_PAIR_STRANDS_MISMATCH) && r1.getReadNegativeStrandFlag == r2.getReadNegativeStrandFlag){
        causeOfDropArray(CODA_PAIR_STRANDS_MISMATCH) += 1;
        return false;
      } else if(dropChrom.contains(r1.getReferenceName())) {
        causeOfDropArray(CODA_READ_ON_IGNORED_CHROMOSOME) += 1;
        return false;
      } else {
        causeOfDropArray(CODA_READ_PAIR_OK) += 1;
        return true;
      }
    }
  }
  def useRead_code(samRecord : SAMRecord, dropOptions : Seq[Boolean], minMAPQ : Int = 255) : Int = {
    if(dropOptions(CODA_IS_NOT_VALID) && samRecord.isValid != null) { return CODA_IS_NOT_VALID; }
    if(dropOptions(CODA_MATE_UNMAPPED) && samRecord.getMateUnmappedFlag()) { return CODA_MATE_UNMAPPED; }
    if(dropOptions(CODA_NOT_PROPER_PAIR) && (! samRecord.getProperPairFlag())) { return CODA_NOT_PROPER_PAIR; }
    if(dropOptions(CODA_READ_FAILS_VENDOR_QC) && samRecord.getReadFailsVendorQualityCheckFlag()){ return CODA_READ_FAILS_VENDOR_QC; }
    if(dropOptions(CODA_NOT_PRIMARY_ALIGNMENT) && samRecord.getNotPrimaryAlignmentFlag()) { return CODA_NOT_PRIMARY_ALIGNMENT; }
    if(dropOptions(CODA_NOT_UNIQUE_ALIGNMENT) && samRecord.getMappingQuality() < minMAPQ) { return CODA_NOT_UNIQUE_ALIGNMENT; }
    return CODA_READ_OK;
  }
  
  //def useRead(samRecord : SAMRecord, causeOfDropArray : Array[Int]) : Boolean = {
  //  if(samRecord.getMateUnmappedFlag()) { causeOfDropArray(0) = causeOfDropArray(0) + 1; return false; }
  //  if(! samRecord.getProperPairFlag()) { causeOfDropArray(1) = causeOfDropArray(1) + 1; return false; }
  //  if(samRecord.getReadFailsVendorQualityCheckFlag()){ causeOfDropArray(2) = causeOfDropArray(2) + 1; return false; }
  //  if(samRecord.getNotPrimaryAlignmentFlag()) { causeOfDropArray(3) = causeOfDropArray(3) + 1; return false; }
  //  if( samRecord.isValid != null) { causeOfDropArray(4) = causeOfDropArray(4) + 1; return false; }
  //  return true;
  //}
  //def useRead(samRecord : SAMRecord) : Boolean = {
  //  if(samRecord.getMateUnmappedFlag()) { return false; }
  //  if(! samRecord.getProperPairFlag()) {  return false; }
  //  if(samRecord.getReadFailsVendorQualityCheckFlag()){  return false; }
  //  if(samRecord.getNotPrimaryAlignmentFlag()) {  return false; }
  //  if( samRecord.isValid != null) { return false; }
  //  return true;
  //}

}