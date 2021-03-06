// This file is generated by CLOPS. Do NOT edit. Instead, edit the
// CLOPS input file or the template.
package ie.ucd.clops.test.generatedinterface;

public abstract class CLOTestAntTaskBase
    extends org.apache.tools.ant.Task {
  private CLOTestOptionStore options;

  public CLOTestAntTaskBase() throws Exception {
    options = new CLOTestOptionStore();
  }

  public void setOutput(String value) 
  throws org.apache.tools.ant.BuildException {
    try {
      options.getOptionByIdentifier("Output")
        .setFromString("Output", value);
    } catch (Exception e) {
      throw new org.apache.tools.ant.BuildException(
        "Can't set Output to " + value + ".\n" + 
        "Error: " + e);
    }
  }
  public void setCompile(String value) 
  throws org.apache.tools.ant.BuildException {
    try {
      options.getOptionByIdentifier("Compile")
        .setFromString("Compile", value);
    } catch (Exception e) {
      throw new org.apache.tools.ant.BuildException(
        "Can't set Compile to " + value + ".\n" + 
        "Error: " + e);
    }
  }
  public void setRunTests(String value) 
  throws org.apache.tools.ant.BuildException {
    try {
      options.getOptionByIdentifier("RunTests")
        .setFromString("RunTests", value);
    } catch (Exception e) {
      throw new org.apache.tools.ant.BuildException(
        "Can't set RunTests to " + value + ".\n" + 
        "Error: " + e);
    }
  }
  public void setDebug(String value) 
  throws org.apache.tools.ant.BuildException {
    try {
      options.getOptionByIdentifier("Debug")
        .setFromString("Debug", value);
    } catch (Exception e) {
      throw new org.apache.tools.ant.BuildException(
        "Can't set Debug to " + value + ".\n" + 
        "Error: " + e);
    }
  }
  public void setInput(String value) 
  throws org.apache.tools.ant.BuildException {
    try {
      options.getOptionByIdentifier("Input")
        .setFromString("Input", value);
    } catch (Exception e) {
      throw new org.apache.tools.ant.BuildException(
        "Can't set Input to " + value + ".\n" + 
        "Error: " + e);
    }
  }

  @Override
  public void execute() throws org.apache.tools.ant.BuildException {
    run(options);
  }

  public abstract void run(CLOTestOptionsInterface options)
  throws org.apache.tools.ant.BuildException;
}

