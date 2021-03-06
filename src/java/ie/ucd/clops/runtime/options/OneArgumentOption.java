package ie.ucd.clops.runtime.options;

import ie.ucd.clops.runtime.options.exception.InvalidOptionPropertyValueException;
import ie.ucd.clops.runtime.options.exception.InvalidOptionValueException;
import ie.ucd.clops.runtime.parser.ProcessingResult;

import java.util.Collection;
import java.util.LinkedList;

/**
 * Option that requires one argument.
 * Either -op1=value or ... -op1 value ...
 * The form of the argument is determined by {@code ARGUMENTSHAPE} and the form of
 * the separation between the alias and argument is determined by {@code BETWEEN}.
 * 
 * @author Fintan
 * @author Mikolas Janota
 *
 */
public abstract class OneArgumentOption<T> extends BasicOption<T> {
  private static final String DEFAULTVAL="defaultvalue";
  private static final String BETWEEN="between";
  private static final String ARGUMENTSHAPE="argumentshape";
  private static final String BLANKPARAMALLOWED="blankparamallowed";
  protected static final String ARGUMENTNAME="argumentname";
  private static final String DEFAULT_ARGUMENTNAME="<arg>";


  private String between = "[=" + SEP + "]"; // a regex separating the prefix from the argument
  private String argumentShape = "[^" + SEP + "]*"; // format of the argument
  private String defaultVal = ""; // The value of the argument if none was specified. If set, it's advisable to remove SEP from between.

  private String argumentName;
  private boolean blankparamAllowed;

  public OneArgumentOption(String identifier, String prefix) {
    super(identifier, prefix);
    updateSuffix();
    blankparamAllowed = false;
    argumentName = DEFAULT_ARGUMENTNAME;
  }

  //Static for space/time efficiency (we don't need one per instance) 
  private static Collection<String> acceptedPropertyNames; 
  protected static Collection<String> getStaticAcceptedPropertyNames() {
    if (acceptedPropertyNames == null) {
      acceptedPropertyNames = new LinkedList<String>();  
      acceptedPropertyNames.addAll(BasicOption.getStaticAcceptedPropertyNames());
      acceptedPropertyNames.add(BETWEEN);
      acceptedPropertyNames.add(ARGUMENTSHAPE);
      acceptedPropertyNames.add(DEFAULTVAL);
      acceptedPropertyNames.add(BLANKPARAMALLOWED);
      acceptedPropertyNames.add(ARGUMENTNAME);
    }
    return acceptedPropertyNames;
  }

  @Override
  public Collection<String> getAcceptedPropertyNames() {
    return getStaticAcceptedPropertyNames();
  }

  @Override
  public void setProperty(String propertyName, String propertyValue) throws InvalidOptionPropertyValueException {
    if (propertyName.equalsIgnoreCase(BETWEEN)) {
      setBetween(propertyValue);
    } else if (propertyName.equalsIgnoreCase(ARGUMENTSHAPE)) {
      setArgumentShape(propertyValue);
    } else if (propertyName.equalsIgnoreCase(DEFAULTVAL)) {
      setDefaultVal(propertyValue);   
    } else if (propertyName.equalsIgnoreCase(BLANKPARAMALLOWED)) {
      setBlankParamAllowed(propertyValue);
    } else if (propertyName.equalsIgnoreCase(ARGUMENTNAME)) {
      this.argumentName = propertyValue;
    } else {
      super.setProperty(propertyName, propertyValue);
    }
  }

  private void setBlankParamAllowed(String propertyValue) {
    blankparamAllowed = Boolean.parseBoolean(propertyValue);
  }

  private void setDefaultVal(String newDefaultVal) {
    defaultVal = newDefaultVal;
  }

  @Override
  public void setFromString(String optionAlias, String valueString) throws InvalidOptionValueException {
    valueString = convertToDefaultIfNecessary(optionAlias, valueString);
    super.setFromString(optionAlias, valueString);
  }
  
  protected String convertToDefaultIfNecessary(String optionAlias, String valueString) throws InvalidOptionValueException {
    if (valueString==null || valueString.equals("") ) {
      if (blankparamAllowed) valueString = defaultVal; 
      else throw new InvalidOptionValueException("Parameter expected for " + optionAlias);
    }
    return valueString;
  }

  public void setBetween(String newBetween) {
    between = BasicOption.sanitizePrefix(newBetween);
    updateSuffix();
  }

  public void setArgumentShape(String newArgumentShape) {
    argumentShape = newArgumentShape;
    updateSuffix();
  }

  /** Returns the string that matched the value of the option's
   * argument. The implementation is dependent on {@code
   * updateSuffix}, which determines the groups. */
  public String getMatchingValueString() {
    return match.group(2);
  }

  private void updateSuffix() {
    //"(?:" here denotes a non-capturing group 
    setMatchingSuffix("(?:" + between + "(" + argumentShape + "))?" + SEP);
  }

  /** Name of the placeholder used in documentaion to refer to the
   * argument, the defauls name can be changed by the "argumentname"
   * property. */
  public final String getArgumentName() {
    return argumentName;
  }
}
