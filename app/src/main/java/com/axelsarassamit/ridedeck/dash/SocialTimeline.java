package com.axelsarassamit.ridedeck.dash;

import java.util.*;

/** Half-open capture-time intersections, independent of file numbering and camera rollovers. */
final class SocialTimeline {
    static final class Segment {
        final String path; final long startUs, durationUs;
        Segment(String path,long startUs,long durationUs) { this.path=path;this.startUs=startUs;this.durationUs=durationUs; }
    }
    static final class Slice {
        final String path; final long sourceStartUs,durationUs,outputStartUs;
        Slice(String path,long sourceStartUs,long durationUs,long outputStartUs) {
            this.path=path;this.sourceStartUs=sourceStartUs;this.durationUs=durationUs;this.outputStartUs=outputStartUs;
        }
    }
    static List<Slice> intersect(long startUs,long durationUs,List<Segment> front,long shiftUs) {
        if(durationUs<=0)throw new IllegalArgumentException("Empty trim");
        List<Segment> ordered=new ArrayList<>(front);ordered.sort(Comparator.comparingLong(s->s.startUs));
        List<Slice> result=new ArrayList<>();long cursor=startUs,end=Math.addExact(startUs,durationUs);
        for(Segment s:ordered) {
            long begin=Math.addExact(s.startUs,shiftUs),finish=Math.addExact(begin,s.durationUs);
            long from=Math.max(cursor,begin),until=Math.min(end,finish);
            if(until<=from)continue;
            result.add(new Slice(s.path,from-begin,until-from,from-startUs));cursor=until;
            if(cursor>=end)break;
        }
        return result;
    }
    static boolean visible(List<Slice> slices,long outputUs) {
        for(Slice s:slices)if(outputUs>=s.outputStartUs&&outputUs<s.outputStartUs+s.durationUs)return true;
        return false;
    }
}
