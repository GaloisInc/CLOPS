import java.util.*;

public abstract class OptionDefaults implements IDefault  {
    private final String explanation;
    
    public OptionDefaults(String explanation) {
        this.explanation = explanation;
    }
}