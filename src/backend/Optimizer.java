package backend;

import java.math.BigInteger;

public class Optimizer {
    public static int N = 32;

    public static Multiplier chooseMultiplier (int d,int p) {
        assert d != 0;
        int l = new BigInteger(String.valueOf(d)).subtract(BigInteger.ONE).bitLength();
        int sh = l;
        long low = (long) Math.floor(Math.pow(2, N+l)/d);
        long high = (long) Math.floor((Math.pow(2, N+l) + Math.pow(2, N+l-p))/d);
        // System.out.println(low + " " + high);
        while ((low >> 1) < (high >> 1) && sh > 0) {
            low >>= 1;
            high >>= 1;
            --sh;
        }
        return new Multiplier(high,sh,l);
    }
}
