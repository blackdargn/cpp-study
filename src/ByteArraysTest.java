import com.example.ByteArrays;

import org.junit.Assert;
import org.junit.Test;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Akon-Home on 15/7/8.
 */

public class ByteArraysTest {

    @Test
    public void testBytes(){
        final int[] lens = {2, 4, 8, 16, 48, 56, 82, 102, 182, 248, 364, 1024};
        final Random random = new Random(387346247l);
        final long maxTimes = 100l;
        final AtomicInteger ok = new AtomicInteger(0);
        final AtomicInteger total = new AtomicInteger(0);

        Runnable testRun = new Runnable() {
            int index = 0;
            @Override
            public void run() {
                int count = 0;
                System.out.println("run ...");
                while(count < maxTimes){
                    index = Math.abs(random.nextInt(lens.length));
                    ByteArrays.Bytes one = ByteArrays.getInstance().take(lens[index]);
                    Assert.assertTrue(one != null);
                    try {
                        Thread.sleep(random.nextInt(1000));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    one.release();

                    count++;
                    if(total.incrementAndGet() % 10 == 0){
                        ByteArrays.getInstance().dump();
                    }
                }
                ok.incrementAndGet();
                System.out.println("run ... exit " + ok.get());
            }
        };
        runManyThread(4, testRun);

        while (true && ok.get() < lens.length){
            try {
                Thread.sleep(4000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        ByteArrays.getInstance().dump();
    }

    private void runManyThread(int n, Runnable runnable){
        for(int i=0; i<n; i++){
            new Thread(runnable).start();
        }
    }

}
