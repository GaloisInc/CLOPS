package ie.ucd.clops.codegeneration;

import ie.ucd.clops.codegeneration.GeneratedCodeUnit.Visibility;
import ie.ucd.clops.dsl.parser.DSLInformation;
import ie.ucd.clops.dsl.structs.AssignmentDescription;
import ie.ucd.clops.dsl.structs.FlyRuleDescription;
import ie.ucd.clops.dsl.structs.OptionDescription;
import ie.ucd.clops.dsl.structs.OptionGroupDescription;
import ie.ucd.clops.logging.CLOLogger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.logging.Level;

public class CodeGenerator {


  public static void createCode(DSLInformation dslInfo,
      File outputDir) {

    String parserName = dslInfo.getParserName();
    String formatString = dslInfo.getFormatString();
    Collection<OptionDescription> opDescriptions = dslInfo.getOptionDescriptions();
    Collection<OptionGroupDescription> opGroupDescriptions = dslInfo.getOptionGroupDescriptions();
    Collection<FlyRuleDescription> overrideRuleDescriptions = dslInfo.getFlyRuleDescriptions();
    
    //Specific parser
    GeneratedClassOrInterface specificParser = createSpecificParser(parserName, formatString, opDescriptions, opGroupDescriptions, overrideRuleDescriptions, dslInfo.getPackageName());
    
    //OptionsInterface
    GeneratedClassOrInterface optionsInterface = createOptionsInterface(parserName, opDescriptions, dslInfo.getPackageName());
    
    //SpecifiOptionsStore
    GeneratedClassOrInterface specificOptionStore = createSpecificOptionStore(parserName, opDescriptions, opGroupDescriptions, dslInfo.getPackageName());
    
    try {
      writeGeneratedClasses(outputDir, specificParser, optionsInterface, specificOptionStore);
    } catch (FileNotFoundException fnfe) {
      CLOLogger.getLogger().log(Level.SEVERE, "Error creating file. " + fnfe);
    }
    
  }
  
  private static void addNecessaryImports(GeneratedClassOrInterface genClass) {
    final String pack = "ie.ucd.clops.runtime.options";
    genClass.addImport(pack + ".Option");
    genClass.addImport(pack + ".OptionGroup");
    genClass.addImport(pack + ".OptionStore");
    genClass.addImport(pack + ".BooleanOption");
    genClass.addImport(pack + ".OptionAssignment");
    final String pack2 = "ie.ucd.clops.runtime.flyrules";
    genClass.addImport(pack2 + ".FlyRuleStore");
  }
  
  private static void writeGeneratedClasses(File outputDir, GeneratedClassOrInterface... classes) throws FileNotFoundException {
    for (GeneratedClassOrInterface genClass : classes) {
      writeGeneratedClass(outputDir, genClass);
    }
  }
  
  private static void writeGeneratedClass(File outputDir, GeneratedClassOrInterface... genClasses) throws FileNotFoundException {
    for (GeneratedClassOrInterface genClass : genClasses) {
      PrintWriter pw = new PrintWriter(new FileOutputStream(outputDir.getAbsolutePath() + File.separator + genClass.getName() + ".java"));
      new GeneratedCodePrinter(pw).printClass(genClass);
      pw.flush();
      pw.close();
    }
  }
  
  private static GeneratedMethod createOptionInitialisationMethod(
      String parserName,
      String packageName,
      GeneratedClassOrInterface specificParser, 
      Collection<OptionDescription> opDescriptions, 
      Collection<OptionGroupDescription> opGroupDescriptions) {
  
  GeneratedMethod createOps = new GeneratedMethod("createOptionStore", getQualifiedClassName(parserName + "OptionStore", packageName), Visibility.Private);
    //specificParser.addMethod(createOps);
    createOps.addException("ie.ucd.clops.runtime.options.InvalidOptionPropertyValueException");
    
    createOps.addStatement("return new " + parserName + "OptionStore()");
    
    return createOps;
  }
  
  private static void createFlyRuleInitialisationCode(GeneratedClassOrInterface specificParser, Collection<FlyRuleDescription> overrideRuleDescriptions) {
    
    GeneratedMethod createOverrideRules = new GeneratedMethod("createFlyRuleStore", "FlyRuleStore", Visibility.Private);
    specificParser.addMethod(createOverrideRules);
    
    createOverrideRules.addStatement("FlyRuleStore flyStore = new FlyRuleStore()");
    
    for (FlyRuleDescription orDescription : overrideRuleDescriptions) {
      String opId = orDescription.getTriggeringOptionIdentifier();
      for (AssignmentDescription desc : orDescription.getAssignments()) {
        createOverrideRules.addStatement("flyStore.addAssignmentForOption(\"" + opId + "\", new OptionAssignment(\"" + desc.getOptionIdentifier() + "\", \"" + desc.getValue() + "\"))");
      }
    }
    
    createOverrideRules.addStatement("return flyStore");
  }
  
  private static GeneratedClassOrInterface createOptionsInterface(String parserName, Collection<OptionDescription> opDescriptions, String outputPackage) {
    GeneratedClassOrInterface optionsInterface = new GeneratedClassOrInterface(parserName + "OptionsInterface", true, outputPackage, Visibility.Public);
    for (OptionDescription od : opDescriptions) {
      GeneratedMethod isSetMethod = new GeneratedMethod("is" + od.getIdentifier() + "Set", "boolean", Visibility.Public);
      isSetMethod.setAbstract(true);
      GeneratedMethod getValueMethod = new GeneratedMethod("get" + od.getIdentifier(), od.getType().getOptionValueTypeClass(), Visibility.Public);
      getValueMethod.setAbstract(true);
      optionsInterface.addMethod(isSetMethod);
      optionsInterface.addMethod(getValueMethod);
    }
    return optionsInterface;
  }

  private static GeneratedClassOrInterface createSpecificParser(
      String parserName, 
      String formatString,
      Collection<OptionDescription> opDescriptions, 
      Collection<OptionGroupDescription> opGroupDescriptions,
      Collection<FlyRuleDescription> overrideRuleDescriptions,
      String outputPackage) {
    GeneratedClassOrInterface specificParser = new GeneratedClassOrInterface(parserName + "Parser", false, outputPackage, Visibility.Public);
    addNecessaryImports(specificParser);

    GeneratedField optionStore = new GeneratedField("optionStore", getQualifiedClassName(parserName + "OptionStore", outputPackage));
    optionStore.addModifier("final");
    specificParser.addField(optionStore);
    specificParser.addMethod(createGetter(optionStore));
    GeneratedField flyRuleStore = new GeneratedField("flyRuleStore", "ie.ucd.clops.runtime.flyrules.FlyRuleStore");
    optionStore.addModifier("final");
    specificParser.addMethod(createGetter(flyRuleStore));
    specificParser.addField(flyRuleStore);
    
    specificParser.setSuperClass("ie.ucd.clops.runtime.parser.AbstractSpecificCLParser");
    GeneratedMethod constructor = new GeneratedConstructor(parserName + "Parser", Visibility.Public);
    constructor.addException("ie.ucd.clops.runtime.options.InvalidOptionPropertyValueException");
    constructor.addStatement("optionStore = createOptionStore()");
    constructor.addStatement("flyRuleStore = createFlyRuleStore()");
    specificParser.addMethod(constructor);
    
    //Create the method that will initialise the OptionStore
    specificParser.addMethod(createOptionInitialisationMethod(parserName, outputPackage, specificParser, opDescriptions, opGroupDescriptions));
    
    //Create the method that will initialise the override rules
    createFlyRuleInitialisationCode(specificParser, overrideRuleDescriptions);

    GeneratedMethod createFormat = new GeneratedMethod("getFormatString", "String", Visibility.Public);
    createFormat.addStatement("return \"" + formatString + "\"");
    specificParser.addMethod(createFormat);
    return specificParser;
  }
  
  private static GeneratedClassOrInterface createSpecificOptionStore(
      String parserName,
      Collection<OptionDescription> opDescriptions, 
      Collection<OptionGroupDescription> opGroupDescriptions,
      String outputPackage) {
    final String className = "OptionStore";
    
    GeneratedClassOrInterface specificOptionStore = new GeneratedClassOrInterface(parserName + className, false, outputPackage, Visibility.Public);
    specificOptionStore.setSuperClass("ie.ucd.clops.runtime.options.OptionStore");
    specificOptionStore.addImplementedInterface(parserName + "OptionsInterface");
    final String pack = "ie.ucd.clops.runtime.options";
    specificOptionStore.addImport(pack + ".OptionGroup");
    
    for (OptionDescription opDesc : opDescriptions) {
      GeneratedField field = new GeneratedField(opDesc.getIdentifier(), opDesc.getType().getOptionTypeClass(), Visibility.Private);
      field.addModifier("final");
      specificOptionStore.addField(field);
    }
    
    GeneratedConstructor constructor = new GeneratedConstructor(parserName + className, Visibility.Public);
    constructor.addException("ie.ucd.clops.runtime.options.InvalidOptionPropertyValueException");
    
    //Create and add each Option
    for (OptionDescription opDesc : opDescriptions) {
      constructor.addStatement(opDesc.getIdentifier() + " = new " + opDesc.getType().getOptionTypeClass() + "(\"" + opDesc.getIdentifier() + "\")"); 

      for (String alias : opDesc.getAliases()) {
        constructor.addStatement(opDesc.getIdentifier() + ".addAlias(\"" + alias + "\")");
      }
      for (Entry<String,String> entry : opDesc.getProperties().entrySet()) {
        constructor.addStatement(opDesc.getIdentifier() + ".setProperty(\"" + entry.getKey() + "\",\"" + entry.getValue() + "\")");
      }
      constructor.addStatement("addOption(" + opDesc.getIdentifier() + ")");
    }
    
    //Create and add each OptionGroup
    for (OptionGroupDescription opGroupDesc : opGroupDescriptions) {
      constructor.addStatement("OptionGroup " + opGroupDesc.getIdentifier() + " = new OptionGroup(\"" + opGroupDesc.getIdentifier() + "\")");
      constructor.addStatement("addOptionGroup(" + opGroupDesc.getIdentifier() + ")");
    }
    
    //Loop again, this time adding the children
    for (OptionGroupDescription opGroupDesc : opGroupDescriptions) {
      for (String child : opGroupDesc.getChildren()) {
        constructor.addStatement(opGroupDesc.getIdentifier() + ".addOptionOrGroup(" + child + ")");
      }
    }

    specificOptionStore.addMethod(constructor);
    
    
    for (OptionDescription od : opDescriptions) {
      GeneratedMethod isSetMethod = new GeneratedMethod("is" + od.getIdentifier() + "Set", "boolean", Visibility.Public);
      isSetMethod.addStatement("return " + od.getIdentifier() + ".hasValue()");
      GeneratedMethod getValueMethod = new GeneratedMethod("get" + od.getIdentifier(), od.getType().getOptionValueTypeClass(), Visibility.Public);
      getValueMethod.addStatement("return " + od.getIdentifier() + '.' + od.getType().getOptionValueGetterMethodName() + "()");
      specificOptionStore.addMethod(isSetMethod);
      specificOptionStore.addMethod(getValueMethod);
    }
    
    return specificOptionStore;
  }
 
  private static GeneratedMethod createGetter(GeneratedField field) {
    String methodName = "get" + Character.toUpperCase(field.getName().charAt(0)) + field.getName().substring(1);
    GeneratedMethod method = new GeneratedMethod(methodName, field.getType(), Visibility.Public);
    method.addStatement("return " + field.getName());
    return method;
  }
  
  private static String getQualifiedClassName(String name, String packageName) {
    if (packageName.equals("")) {
      return name;
    } else {
      return packageName + '.' + name;
    }
  }
  
}
