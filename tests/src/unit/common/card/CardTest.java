package unit.common.card;

/**
 * Created by xs on 02-10-2014.
 */
public class CardTest {
    public static boolean equalsAndHash(boolean expected, Object obj, Object obj2){
        return (obj.equals(obj2) == expected) && (obj.hashCode() == obj2.hashCode() == expected);
    }
}
