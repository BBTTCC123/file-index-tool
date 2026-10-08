package chen;


import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DirscannerTest {

    @ParameterizedTest
    @CsvSource({
            "a.txt,      a,          txt",
            ".gitignore, .gitignore, file",
            "a.b.txt,   a.b,         txt",
            "noext,     noext,       file",
            "'',        '',          file",
            "a.,        a,           ''"
    })
    void splits(String wholeName, String expectedName, String expectedExt) {
        String[] r = Dirscanner.extract(wholeName);
        assertEquals(expectedName, r[0]);
        assertEquals(expectedExt,  r[1]);
    }
}