package sqlrunner.flatfileimport;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import sqlrunner.datamodel.SQLField;
import sqlrunner.flatfileimport.generator.AutoValueGenerator;
import sqlrunner.flatfileimport.generator.NumberValueGenerator;
import sqlrunner.flatfileimport.gui.Messages;

/**
 * beinhaltet alle Information zu einer Spalte einer Tabelle in der RS
 * # columnname: Spaltenname in Zieltabelle
 * # index: Index für die Reihenfolge des Einlesens
 * # delimPos: ab dem n.-mal des Auftretens des Trennzeichen, fehlt die Längenangabe, so wird das nächste Trennzeichen gesucht.
 * # delimString: Trennzeichenkette
 * # absPos: ab der absoluten position wird nicht ausgewertet, wenn delimiterPos angegeben
 * # length: Länge, ist nur die Länge angegeben werden ab der letzten position die Zeichen eingelesen
 * # disabled: (Feld wird zwar berücksichtigt beim Parsen der Quelldatei aber nicht importiert)
 */
public class FieldDescription implements Comparable<FieldDescription> {

    private String name;
    private int basicType                      = -1;
    private String format                         = ""; //$NON-NLS-1$
    private static final String DATE_FORMAT_TEMPLATE="dd.MM.yyyy"; //$NON-NLS-1$
    private int index                          = 0;
    private int delimPos                       = -1;
    private int absPos                         = -1;
    private int length                         = 0;
    private String defaultValue                   = ""; //$NON-NLS-1$
    private String alternativeFieldDescriptionName = null;
    private String generatorStartValue = null;
    private boolean isPrimaryKey                   = false;
    private boolean aggregateNumberValues          = false;
    private boolean enabled                        = true;
    private boolean nullEnabled                    = true;
    private boolean ignoreDatasetIfInvalid               = false;
    private boolean trimRequired                           = false;
    static public final int ABSOLUTE_POSITION              = 0;
    static public final int RELATIVE_POSITION              = 1;
    static public final int DELIMITER_POSITION             = 2;
    static public final int DELIMITER_POSITION_WITH_LENGTH = 3;
    static public final int AUTO_GENERATED                 = 4;
    static public final int FIX_VALUE                      = 5;
    private int positionType                   = -1;
    private boolean valid;
    private String errorMessage;
    public static final String sep                            = System.getProperty("line.separator"); //$NON-NLS-1$
    private AutoValueGenerator autoValueGenerator = null;
    private String filterRegex;
    private Pattern filterPattern = null;
    private String regexCompilerMessage;
    private FieldDescription alternativeValueFieldDescription = null;
    private Locale locale = Locale.getDefault();
    private NumberFormat nf = null;
    
    public FieldDescription() {}

    public FieldDescription(
            String name,
            int basicType,
            String format,
            int index,
            int positionType,
            int delimPos,
            int absPos,
            int length,
            boolean isPrimaryKey,
            boolean enabled,
            String defaultValue,
            boolean aggregate,
            boolean nullEnabled,
            String generatorStartValue) {
        if (name != null && name.trim().length() == 0) {
            name = null;
        }
        this.name = name;
        this.basicType = basicType;
        this.format = format;
        this.index = index;
        this.delimPos = delimPos;
        this.absPos = absPos;
        this.length = length;
        this.isPrimaryKey = isPrimaryKey;
        this.enabled = enabled;
        if (defaultValue != null) {
            this.defaultValue = defaultValue;
        }
        if (positionType != -1) {
            this.positionType = positionType;
        } else {
            computePositionType();
        }
        if ((basicType == BasicDataType.DATE.getId()) && ((format == null) || (format.trim().length() < 2))) {
            this.format = DATE_FORMAT_TEMPLATE;
        }
        if (isPrimaryKey && (enabled == false)) {
            this.enabled = true;
        }
        this.aggregateNumberValues = aggregate;
        this.nullEnabled = nullEnabled;
        this.generatorStartValue = generatorStartValue;
        setupNumberFormat();
    }
    
    public FieldDescription(SQLField field) {
    	this.name = field.getName();
    	this.basicType = field.getBasicType();
        if ((basicType == BasicDataType.DATE.getId())) {
            this.format = DATE_FORMAT_TEMPLATE;
        } else {
        	this.format = "";
        }
    	this.index = field.getOrdinalPosition();
    	this.delimPos = field.getOrdinalPosition() - 1;
    	this.absPos = -1;
    	this.length = field.getLength();
    	this.isPrimaryKey = field.isPrimaryKey();
    	this.enabled = true;
    	this.positionType = FieldDescription.DELIMITER_POSITION;
    	this.aggregateNumberValues = false;
    	this.nullEnabled = true; 
        setupNumberFormat();
    }
    
    public FieldDescription(int index, int propertySearchNumber, Properties properties) {
        this.index = index;
    	fillFromProperties(propertySearchNumber, properties);
        if (positionType == -1) {
            computePositionType();
        }
        if ((basicType == BasicDataType.DATE.getId()) && ((format == null) || (format.trim().length() < 2))) {
            this.format = DATE_FORMAT_TEMPLATE;
        }
        if (isPrimaryKey && (enabled == false)) {
            this.enabled = true;
        }
        setupNumberFormat();
    }

    private static Locale createLocale(String localeName) {
        if (localeName == null || localeName.length() == 0) {
            localeName = "en_US";
        }
        Locale locale = null;
        int pos = localeName.indexOf('_');
        if (pos > 1) {
            String language = localeName.substring(0, pos);
            String country = localeName.substring(pos + 1);
            locale = new Locale(language, country);
        } else {
            locale = new Locale(localeName);
        }
        return locale;
    }
    
    private void setupNumberFormat() {
        if (BasicDataType.isNumberType(basicType)) {
            if (locale == null) {
                locale = createLocale(format);
            }
            nf = NumberFormat.getInstance(locale);
        }
    }

    private void setupDateFormat() {
        if (basicType == BasicDataType.DATE.getId()) {
            if (format != null) {
                if (locale != null) {
                    new SimpleDateFormat(format, locale);
                } else {
                    new SimpleDateFormat(format);
                }
            }
        }
    }
    
    public Locale getNumberFormatLocale() {
        return locale;
    }
    
    public NumberFormat getNumberFormat() {
        return nf;
    }

    /**
     * FieldDescription which will be applied if own value is null (after check the defaultValue)
     * @return FieldDescription
     */
    public FieldDescription getAlternativeFieldDescription() {
        return alternativeValueFieldDescription;
    }

    public void setAlternativeFieldDescription(FieldDescription alternativeValueFieldDescription) {
        this.alternativeValueFieldDescription = alternativeValueFieldDescription;
    }
    
    public String getAlternativeFieldDescriptionName() {
        return alternativeFieldDescriptionName;
    }

    public void setAlternativeFieldDescriptionName(String alternativeFieldDescriptionName) {
        this.alternativeFieldDescriptionName = alternativeFieldDescriptionName;
    }
    
    public boolean isDummy() {
        return getName().startsWith("#");
    }
    
    public String getName() {
        if (name != null) {
            return name;
        } else {
            return "#Test-" + this.delimPos;
        }
    }

    public void setName(String name_loc) {
    	if (name_loc != null && name_loc.isEmpty() == false) {
    		name = name_loc;
    	}
    }

    public int getBasicTypeId() {
        return basicType;
    }

    public void setBasicTypeId(int basicType_loc) {
        this.basicType = basicType_loc;
        setupNumberFormat();
        setupDateFormat();
    }

    public String getFieldFormat() {
        return format;
    }

    public void setFormat(String format_loc) {
        if (format_loc != null && format_loc.trim().length() == 0) {
            this.format = null;
        } else {
            this.format = format_loc;
        }
        setupNumberFormat();
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index_loc) {
        this.index = index_loc;
    }

    public int getDelimPos() {
        return delimPos;
    }

    public void setDelimPos(int delimPos_loc) {
        this.delimPos = delimPos_loc;
    }

    public int getAbsPos() {
        return absPos;
    }

    public void setAbsPos(int absPos_loc) {
        this.absPos = absPos_loc;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length_loc) {
        this.length = length_loc;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled_loc) {
        this.enabled = enabled_loc;
    }

    public int getPositionType() {
        return positionType;
    }

    public void setPositionType(int positionType_loc) {
        this.positionType = positionType_loc;
    }

    public boolean validate() {
        valid = true;
        if (isPrimaryKey && (enabled == false)) {
            errorMessage = Messages.getString("FieldDescription.4"); //$NON-NLS-1$
            valid = false;
        } else if ((name == null) || (name.length() == 0)) {
            errorMessage = Messages.getString("FieldDescription.5"); //$NON-NLS-1$
            valid = false;
        } else if ((basicType == BasicDataType.DATE.getId()) && ((format == null) || (format.trim().length() < 2))) {
            errorMessage = Messages.getString("FieldDescription.6"); //$NON-NLS-1$
            valid = false;
        } else if (basicType == -1) {
            errorMessage = Messages.getString("FieldDescription.7"); //$NON-NLS-1$
            valid = false;
        } else if (filterRegex != null && filterPattern == null) {
            errorMessage = "filterRegex: " + regexCompilerMessage;
        	valid = false;
        } else {
            if (positionType == -1) {
                errorMessage = Messages.getString("FieldDescription.8"); //$NON-NLS-1$
                valid = false;
            } else {
                switch (positionType) {
                    case FieldDescription.ABSOLUTE_POSITION:
                        if (absPos == -1) {
                            errorMessage = Messages.getString("FieldDescription.9"); //$NON-NLS-1$
                            valid = false;
                        } else if (length == 0) {
                            errorMessage = Messages.getString("FieldDescription.10"); //$NON-NLS-1$
                            valid = false;
                        }
                        break;
                    case FieldDescription.RELATIVE_POSITION:
                        if (length == 0) {
                            errorMessage = Messages.getString("FieldDescription.11"); //$NON-NLS-1$
                            valid = false;
                        }
                        break;
                    case FieldDescription.DELIMITER_POSITION_WITH_LENGTH:
                        if (delimPos == -1) {
                            errorMessage = Messages.getString("FieldDescription.12"); //$NON-NLS-1$
                            valid = false;
                        } else if (length == 0) {
                            errorMessage = Messages.getString("FieldDescription.13"); //$NON-NLS-1$
                            valid = false;
                        }
                        break;
                    case FieldDescription.DELIMITER_POSITION:
                        if (delimPos == -1) {
                            errorMessage = Messages.getString("FieldDescription.14"); //$NON-NLS-1$
                            valid = false;
                        }
                        break;
                } // switch (positionType)
            } // if (positionType == -1)
        } // if (name == null)
        return valid;
    }

    public boolean isPartOfPrimaryKey() {
        return isPrimaryKey;
    }

    public void setIsPartOfPrimaryKey(boolean isPartOfPrimaryKey) {
        this.isPrimaryKey = isPartOfPrimaryKey;
    }

    private void computePositionType() {
        if (basicType == -1) {
            valid = false;
            errorMessage = Messages.getString("FieldDescription.15"); //$NON-NLS-1$
        } else {
            if (basicType == BasicDataType.DATE.getId()) {
                if (format != null) {
                    valid = true;
                } else {
                    valid = false;
                    errorMessage = Messages.getString("FieldDescription.16"); //$NON-NLS-1$
                }
            } // if (basicType == BASICTYPE_DATE)
        }
        if (absPos != -1) {
            positionType = ABSOLUTE_POSITION;
            if (length != 0) {
                valid = true;
            } else {
                valid = false;
                errorMessage = Messages.getString("FieldDescription.17"); //$NON-NLS-1$
            }
        } else if (delimPos != -1) {
            if (length != 0) {
                positionType = DELIMITER_POSITION_WITH_LENGTH;
            } else {
                positionType = DELIMITER_POSITION;
            }
        } else if (length != 0) {
            positionType = RELATIVE_POSITION;
            valid = true;
        } else {
            valid = false;
            errorMessage = Messages.getString("FieldDescription.18"); //$NON-NLS-1$
        }
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public int compareTo(FieldDescription object) {
    	if (object != null) {
            return index - object.getIndex();
    	} else {
    		return 0;
    	}
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String value) {
        this.defaultValue = value;
    }
    
    public String getNextAutoGeneratedValue() {
    	setupGenerator();
    	if (autoValueGenerator != null) {
    		return autoValueGenerator.getNext();
    	} else {
    		return null;
    	}
    }
    
    private void setupGenerator() {
    	if (autoValueGenerator == null) {
        	if (BasicDataType.isNumberType(basicType)) {
        		autoValueGenerator = new NumberValueGenerator();
        		if (generatorStartValue != null && generatorStartValue.length() > 0) {
            		autoValueGenerator.setStartValue(generatorStartValue);
        		}
        	}
    	}
    }
    
    public void resetAutValueGenerator() {
    	if (autoValueGenerator != null) {
    		autoValueGenerator.reset();
    	}
    }

    public String getPropertiesString() {
    	StringBuffer sb = new StringBuffer();
    	sb.append("# -----------------------------");
    	sb.append(sep);
    	Properties props = new Properties();
    	completeProperties(props);
    	Map.Entry<?, ?> entry = null;
    	for (Iterator<?> it = props.entrySet().iterator(); it.hasNext(); ) {
    		entry = (Map.Entry<?,?>) it.next();
    		sb.append(entry.getKey());
    		sb.append('=');
    		sb.append(entry.getValue());
    		sb.append(sep);
    	}
    	return sb.toString();
    }
    
    public void completeProperties(Properties props) {
        if (name != null) {
            props.put("COLUMN_" + String.valueOf(index) + "_NAME", name); //$NON-NLS-1$ //$NON-NLS-2$
            props.put("COLUMN_" + String.valueOf(index) + "_BASICTYPE", String.valueOf(basicType)); //$NON-NLS-1$ //$NON-NLS-2$
            if (basicType == BasicDataType.DATE.getId() && format != null) {
                props.put("COLUMN_" + String.valueOf(index) + "_FORMAT", format); //$NON-NLS-1$ //$NON-NLS-2$
            } else {
                props.remove("COLUMN_" + String.valueOf(index) + "_FORMAT"); //$NON-NLS-1$ //$NON-NLS-2$
            }
            if (locale != null) {
                props.put("COLUMN_" + String.valueOf(index) + "_LOCALE", locale.toString()); //$NON-NLS-1$ //$NON-NLS-2$
            } else {
                props.remove("COLUMN_" + String.valueOf(index) + "_LOCALE"); //$NON-NLS-1$ //$NON-NLS-2$
            }
            props.put("COLUMN_" + String.valueOf(index) + "_POSITIONTYPE", String.valueOf(positionType)); //$NON-NLS-1$ //$NON-NLS-2$
            props.put("COLUMN_" + String.valueOf(index) + "_DELIMITERCOUNT", String.valueOf(delimPos)); //$NON-NLS-1$ //$NON-NLS-2$
            props.put("COLUMN_" + String.valueOf(index) + "_ABSPOS", String.valueOf(absPos)); //$NON-NLS-1$ //$NON-NLS-2$
            props.put("COLUMN_" + String.valueOf(index) + "_LENGTH", String.valueOf(length)); //$NON-NLS-1$ //$NON-NLS-2$
            props.put("COLUMN_" + String.valueOf(index) + "_PRIMARYKEY", String.valueOf(isPrimaryKey)); //$NON-NLS-1$ //$NON-NLS-2$
            props.put("COLUMN_" + String.valueOf(index) + "_ENABLED", String.valueOf(enabled)); //$NON-NLS-1$ //$NON-NLS-2$
            if (defaultValue != null) {
                props.put("COLUMN_" + String.valueOf(index) + "_DEFAULT", defaultValue); //$NON-NLS-1$ //$NON-NLS-2$
            } else {
                props.remove("COLUMN_" + String.valueOf(index) + "_DEFAULT"); //$NON-NLS-1$ //$NON-NLS-2$
            }
            if (alternativeFieldDescriptionName != null) {
                props.put("COLUMN_" + String.valueOf(index) + "_ALTERNATIVE_FIELD", alternativeFieldDescriptionName); //$NON-NLS-1$ //$NON-NLS-2$
            } else {
                props.remove("COLUMN_" + String.valueOf(index) + "_ALTERNATIVE_FIELD"); //$NON-NLS-1$ //$NON-NLS-2$
            }
            props.put("COLUMN_" + String.valueOf(index) + "_AGGREGATE", String.valueOf(aggregateNumberValues)); //$NON-NLS-1$ //$NON-NLS-2$
            props.put("COLUMN_" + String.valueOf(index) + "_NULL_ENABLED", String.valueOf(nullEnabled)); //$NON-NLS-1$ //$NON-NLS-2$
            props.put("COLUMN_" + String.valueOf(index) + "_IGNORE_DATASET_IF_INVALID", String.valueOf(ignoreDatasetIfInvalid)); //$NON-NLS-1$ //$NON-NLS-2$
            props.put("COLUMN_" + String.valueOf(index) + "_TRIM", String.valueOf(trimRequired)); //$NON-NLS-1$ //$NON-NLS-2$
            if (generatorStartValue != null) {
                props.put("COLUMN_" + String.valueOf(index) + "_GENERATORSTARTVALUE", generatorStartValue);
            } else {
                props.remove("COLUMN_" + String.valueOf(index) + "_GENERATORSTARTVALUE");
            }
            if (filterRegex != null) {
                props.put("COLUMN_" + String.valueOf(index) + "_REGEX", filterRegex); //$NON-NLS-1$ //$NON-NLS-2$
            } else {
                props.remove("COLUMN_" + String.valueOf(index) + "_REGEX"); //$NON-NLS-1$ //$NON-NLS-2$
            }
        }
    }

    public void fillFromProperties(int propertySearchNumber, Properties props) {
        name = props.getProperty("COLUMN_" + String.valueOf(propertySearchNumber) + "_NAME"); //$NON-NLS-1$ //$NON-NLS-2$
        basicType = Integer.parseInt(props.getProperty("COLUMN_" + String.valueOf(propertySearchNumber) + "_BASICTYPE", "0")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        setLocale(props.getProperty("COLUMN_" + String.valueOf(propertySearchNumber) + "_LOCALE"));
        format = props.getProperty("COLUMN_" + String.valueOf(propertySearchNumber) + "_FORMAT"); //$NON-NLS-1$ //$NON-NLS-2$
        positionType = Integer.parseInt(props.getProperty("COLUMN_" + String.valueOf(propertySearchNumber) + "_POSITIONTYPE", "0")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        delimPos = Integer.parseInt(props.getProperty("COLUMN_" + String.valueOf(propertySearchNumber) + "_DELIMITERCOUNT", "0")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        absPos = Integer.parseInt(props.getProperty("COLUMN_" + String.valueOf(propertySearchNumber) + "_ABSPOS", "0")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        length = Integer.parseInt(props.getProperty("COLUMN_" + String.valueOf(propertySearchNumber) + "_LENGTH", "0")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        isPrimaryKey = props.getProperty("COLUMN_" + String.valueOf(propertySearchNumber) + "_PRIMARYKEY", "false").equals("true"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        enabled = props.getProperty("COLUMN_" + String.valueOf(propertySearchNumber) + "_ENABLED", "false").equals("true"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        defaultValue = props.getProperty("COLUMN_" + String.valueOf(propertySearchNumber) + "_DEFAULT"); //$NON-NLS-1$ //$NON-NLS-2$
        alternativeFieldDescriptionName = props.getProperty("COLUMN_" + String.valueOf(propertySearchNumber) + "_ALTERNATIVE_FIELD"); //$NON-NLS-1$ //$NON-NLS-2$
        aggregateNumberValues = props.getProperty("COLUMN_" + String.valueOf(propertySearchNumber) + "_AGGREGATE", "false").equals("true"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        nullEnabled = props.getProperty("COLUMN_" + String.valueOf(propertySearchNumber) + "_NULL_ENABLED", "false").equals("true");
        ignoreDatasetIfInvalid = props.getProperty("COLUMN_" + String.valueOf(propertySearchNumber) + "_IGNORE_DATASET_IF_INVALID", "false").equals("true");
        trimRequired = props.getProperty("COLUMN_" + String.valueOf(propertySearchNumber) + "_TRIM", "false").equals("true");
        generatorStartValue = props.getProperty("COLUMN_" + String.valueOf(propertySearchNumber) + "_GENERATORSTARTVALUE"); //$NON-NLS-1$ //$NON-NLS-2$
        setFilterRegex(props.getProperty("COLUMN_" + String.valueOf(propertySearchNumber) + "_REGEX")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    public String getExtractionDescription() {
        String desc;
        if (basicType == BasicDataType.SQLEXP.getId()) {
            desc = "SQL code";
        } else {
            switch (positionType) {
                case ABSOLUTE_POSITION:
                    desc = Messages.getString("FieldDescription.68") + String.valueOf(absPos) + Messages.getString("FieldDescription.69") + String.valueOf(length); //$NON-NLS-1$ //$NON-NLS-2$
                    if (alternativeFieldDescriptionName != null) {
                        desc = desc + " altern. " + alternativeFieldDescriptionName;
                    }
                    if (filterPattern != null) {
                        desc = desc + " (filterRegex)";
                    }
                    if (defaultValue != null && defaultValue.length() > 0) {
                        desc = desc + Messages.getString("FieldDescription.78") + defaultValue; //$NON-NLS-1$
                    }
                    break;
                case RELATIVE_POSITION:
                    desc = Messages.getString("FieldDescription.70") + String.valueOf(length); //$NON-NLS-1$
                    if (alternativeFieldDescriptionName != null) {
                        desc = desc + " altern. " + alternativeFieldDescriptionName;
                    }
                    if (filterPattern != null) {
                        desc = desc + " (filterRegex)";
                    }
                    if (defaultValue != null && defaultValue.length() > 0) {
                        desc = desc + Messages.getString("FieldDescription.78") + defaultValue; //$NON-NLS-1$
                    }
                    break;
                case FIX_VALUE:
                    desc = Messages.getString("FieldDescription.fixvalue") + " = " + defaultValue; //$NON-NLS-1$
                    if (alternativeFieldDescriptionName != null) {
                        desc = desc + " altern. " + alternativeFieldDescriptionName;
                    }
                    if (filterPattern != null) {
                        desc = desc + " (filterRegex)";
                    }
                    break;
                case AUTO_GENERATED:
                    if (generatorStartValue != null && generatorStartValue.trim().length() > 0) {
                        desc = Messages.getString("FieldDescription.80") + generatorStartValue; //$NON-NLS-1$
                    } else {
                        desc = Messages.getString("FieldDescription.81"); //$NON-NLS-1$
                    }
                    break;
                case DELIMITER_POSITION:
                    if (delimPos == 0) {
                        desc = Messages.getString("FieldDescription.71"); //$NON-NLS-1$
                    } else {
                        desc = Messages.getString("FieldDescription.72") + delimPos + Messages.getString("FieldDescription.73"); //$NON-NLS-1$ //$NON-NLS-2$
                    }
                    if (alternativeFieldDescriptionName != null) {
                        desc = desc + " altern. " + alternativeFieldDescriptionName;
                    }
                    if (filterPattern != null) {
                        desc = desc + " (filterRegex)";
                    }
                    if (defaultValue != null && defaultValue.length() > 0) {
                        desc = desc + Messages.getString("FieldDescription.78") + defaultValue; //$NON-NLS-1$
                    }
                    break;
                case DELIMITER_POSITION_WITH_LENGTH:
                    if (delimPos == 0) {
                        desc = Messages.getString("FieldDescription.74") + String.valueOf(length); //$NON-NLS-1$
                    } else {
                        desc = Messages.getString("FieldDescription.75") + delimPos + Messages.getString("FieldDescription.76") + String.valueOf(length); //$NON-NLS-1$ //$NON-NLS-2$
                    }
                    if (alternativeFieldDescriptionName != null) {
                        desc = desc + " altern. " + alternativeFieldDescriptionName;
                    }
                    if (filterPattern != null) {
                        desc = desc + " (filterRegex)";
                    }
                    if (defaultValue != null && defaultValue.length() > 0) {
                        desc = desc + Messages.getString("FieldDescription.78") + defaultValue; //$NON-NLS-1$
                    }
                    break;
                default:
                    desc = Messages.getString("FieldDescription.77"); //$NON-NLS-1$
            }
        }
        return desc;
    }

    @Override
	public boolean equals(Object obj) {
    	if (obj instanceof FieldDescription) {
    		String otherName = ((FieldDescription) obj).getName();
    		if (name !=  null && otherName != null && name.toLowerCase().trim().equals(otherName.toLowerCase().trim())) {
    			return true;
    		}
    	}
    	return false;
	}

	@Override
	public int hashCode() {
		// TODO Auto-generated method stub
		return super.hashCode();
	}

    @Override
    public String toString() {
        return name; //$NON-NLS-1$
    }

    public final boolean isAggregateNumberValues() {
        return aggregateNumberValues;
    }

    public final void setAggregateNumberValues(boolean aggregateNumberValues) {
        this.aggregateNumberValues = aggregateNumberValues;
    }

	public boolean isNullEnabled() {
		return nullEnabled && isPrimaryKey == false;
	}

	public void setNullEnabled(boolean nullEnabled) {
		this.nullEnabled = nullEnabled;
	}

    public boolean isIgnoreDatasetIfInvalid() {
		return ignoreDatasetIfInvalid;
	}

	public void setIgnoreDatasetIfInvalid(boolean ignoreDatasetIfInvalid) {
		this.ignoreDatasetIfInvalid = ignoreDatasetIfInvalid;
	}

	public boolean isTrimRequired() {
        return trimRequired;
    }

    public void setTrimRequired(boolean trim) {
        this.trimRequired = trim;
    }

	public String getGeneratorStartValue() {
		return generatorStartValue;
	}

	public void setGeneratorStartValue(String generatorStartValue) {
		this.generatorStartValue = generatorStartValue;
	}

	public static String getPositioningTypeName(int positioningType) {
		switch (positioningType) {
		case ABSOLUTE_POSITION:
			return Messages.getString("FieldDescription.absolute"); //$NON-NLS-1$
		case AUTO_GENERATED:
			return Messages.getString("FieldDescription.generated"); //$NON-NLS-1$
		case DELIMITER_POSITION:
			return Messages.getString("FieldDescription.afternumberdelimiter"); //$NON-NLS-1$
		case DELIMITER_POSITION_WITH_LENGTH:
			return Messages.getString("FieldDescription.afternumberdelimiterwithlength"); //$NON-NLS-1$
		case FIX_VALUE:
			return Messages.getString("FieldDescription.fixvalue"); //$NON-NLS-1$
		case RELATIVE_POSITION:
			return Messages.getString("FieldDescription.following"); //$NON-NLS-1$
		default:
			return "unknown positioning type"; //$NON-NLS-1$
		}
	}

	public String getRegex() {
		return filterRegex;
	}
	
	public Pattern getFilterPattern() {
		return filterPattern;
	}

	public void setFilterRegex(String regex) {
		if (regex != null && regex.trim().length() > 0) {
			this.filterRegex = regex;
	        try {
	            filterPattern = Pattern.compile(regex);
	        } catch (PatternSyntaxException pse) {
	        	filterPattern = null;
	        	regexCompilerMessage = pse.getMessage();
	        }
	    } else {
	    	this.filterRegex = null;
	    	this.filterPattern = null;
	    }
	}

    public void setLocale(String localeString) {
        locale = createLocale(localeString);
        setupNumberFormat();
        setupDateFormat();
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
        setupNumberFormat();
        setupDateFormat();
    }

    public Locale getLocale() {
        return locale;
    }
	
}
