package chen;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class CalTest {
    @Test
    void testAdd() {
        Calculator calc = new Calculator();
        Assertions.assertEquals(5, calc.add(2, 3));
    }
}
