package com.example.exposure;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ExposureRepository extends JpaRepository<ClaimExposure, UUID> {

    /** rows: [market, openClaimCount, sumExposure] for claims that are not rejected or settled. */
    @Query("select e.market, count(e), sum(e.exposureAmount) from ClaimExposure e "
            + "where e.status not in ('REJECTED','SETTLED') group by e.market order by e.market")
    List<Object[]> openByMarket();

    /** rows: [status, count] */
    @Query("select e.status, count(e) from ClaimExposure e group by e.status order by e.status")
    List<Object[]> countByStatus();
}
