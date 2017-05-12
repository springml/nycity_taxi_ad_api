package com.springml.nyc.taxi.ad.api;

import com.springml.nyc.taxi.ad.datastore.AdCountStoreManager;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import static junit.framework.TestCase.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Created by kaarthikraaj on 8/5/17.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = AdApiApplication.class)
public class AdThresholdServiceTest {
    @Mock
    AdCountStoreManager adCountStoreManagerMock;


    @Autowired
    AdThresholdService adThresholdService;



    @Test
    public void testLimitExceededCase(){


        when(adCountStoreManagerMock.getAdUpdateCount("3"))
                .thenReturn(30000);
        adThresholdService.setAdCountStoreManager(adCountStoreManagerMock);
        boolean isLimitExceeded = adThresholdService.isAdThresholdExceeded(3);
        assertTrue(isLimitExceeded);


    }

    @Test
    public void testLimitNotExceededCase(){


        when(adCountStoreManagerMock.getAdUpdateCount("3"))
                .thenReturn(29999);
        adThresholdService.setAdCountStoreManager(adCountStoreManagerMock);
        boolean isLimitExceeded = adThresholdService.isAdThresholdExceeded(3);
        assertTrue(!isLimitExceeded);


    }


}
