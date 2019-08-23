package cz.havranek.opensource.cacheUtils;

import java.util.Optional;

/**
 * Holder of information for cache
 * reports most main level of cache ( all in bytes all optional to maximize compatibility
 */

public class CacheInfo {
    final Optional<Integer> L4_BYTES;
    final Optional<Integer> L3_BYTES;
    final Optional<Integer> L2_SYS_REPORT_BYTES;
    final Optional<Integer> L2_PER_CORE_EST_BYTES;

    /**
    *  stupid constructor.... just get values and converts them into optional
    * */
    CacheInfo(Integer L4, Integer L3,Integer L2_FromSYS,Integer L2_PerCoreEst){
        L4_BYTES=Optional.ofNullable(L4);
        L3_BYTES=Optional.ofNullable(L3);
        L2_SYS_REPORT_BYTES=Optional.ofNullable(L2_FromSYS);
        L2_PER_CORE_EST_BYTES=Optional.ofNullable(L2_PerCoreEst);
    }

    /**
     * @return return true if has some values present
     */
    boolean hasSomeValues(){
        return L4_BYTES.isPresent() || L3_BYTES.isPresent()  || L2_SYS_REPORT_BYTES.isPresent();
    }
}
