package tar.eof.ext6.utils;

import com.weilerhaus.productKeys.enums.ProductKeyState;
import com.weilerhaus.productKeys.impl.BasicProductKeyGenerator;
import com.weilerhaus.productKeys.impl.beans.BasicProductKeyEncodingData;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class KeyChecker {

    ProductKeyState tmpGeneratedKeyState;

    public boolean ValidatorKey(String key){

        BasicProductKeyGenerator basicProductKeyGenerator = new BasicProductKeyGenerator(
                new BasicProductKeyEncodingData((byte) 24, (byte) 3, (byte) 101),
                null,
                new BasicProductKeyEncodingData((byte) 1, (byte) 2, (byte) 91),
                new BasicProductKeyEncodingData((byte) 7, (byte) 1, (byte) 100),
                null,
                null,
                new BasicProductKeyEncodingData((byte) 21, (byte) 67, (byte) 25),
                null,
                new BasicProductKeyEncodingData((byte) 31, (byte) 22, (byte) 34),
                null
        );

        if ((key != null) && (key.trim().length() > 0)) {

            String trueKey = key.substring(0, key.lastIndexOf("-"));

            tmpGeneratedKeyState = basicProductKeyGenerator.verifyProductKey(trueKey);

            String valid_until = key.substring(key.lastIndexOf("-") + 1, key.length());

            Date dateconverted = new Date(Long.parseLong(valid_until.toLowerCase(), 16)*100000);

            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");

            String dateString = sdf.format(dateconverted);

            Date strDate = null;
            try {
                strDate = sdf.parse(dateString);
            } catch (ParseException e) {
                e.printStackTrace();
            }

            if ((ProductKeyState.KEY_GOOD == tmpGeneratedKeyState) && !(new Date().after(strDate))) {
                PreferenceStorage.storeExpiration(valid_until);
                return true;
            }else{
                return false;
            }
        }else {
            return false;
        }
    }
}
