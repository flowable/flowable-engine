package org.flowable.variable.service.impl.persistence.entity;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class AdvancedList<T> extends ArrayList<T> implements Serializable {

	private static final long serialVersionUID = 1L;

	public boolean containsString(String str) {

		SealMetadata tmp = new SealMetadata();
		tmp.setValue(str);

		return this.contains(tmp);

	}

	public boolean containsRegex(String str) {
		if (!isValidRegEx(str)) {
			throw new IllegalArgumentException("Invalid Regex supplied");
		}
		for (Object o : this) {
			if (o instanceof String) {
				if (o.toString().matches(str))
					return true;
			} else if (o instanceof SealMetadata) {
				if (((SealMetadata) o).containsRegex(str)) {
					return true;
				}
			}
		}
		return false;
	}

	public boolean containsNumber(Double number) {
		for (Object o : this) {
			if (o instanceof Double) {
				if ((Double) o == number)
					return true;
			} else if (o instanceof SealMetadata) {
				SealMetadata obj = (SealMetadata) o;
				if (tryParseDouble(obj.getValue())) {
					if (Double.parseDouble(obj.getValue()) == number) {
						return true;
					}
				}
			}
		}
		return false;
	}

	public boolean containsNumberNotEquals(Double number) {
		for (Object o : this) {
			if (o instanceof Double) {
				if ((Double) o != number)
					return true;
			} else if (o instanceof SealMetadata) {
				SealMetadata obj = (SealMetadata) o;
				if (tryParseDouble(obj.getValue())) {
					if (Double.parseDouble(obj.getValue()) != number) {
						return true;
					}
				}
			}
		}
		return false;
	}

	public boolean containsNumberLessThan(Double number) {
		for (Object o : this) {
			if (o instanceof Number) {
				if ((Double) o < number)
					return true;
			} else if (o instanceof SealMetadata) {
				SealMetadata obj = (SealMetadata) o;
				if (tryParseDouble(obj.getValue())) {
					if (Double.parseDouble(obj.getValue()) < number) {
						return true;
					}
				}
			}
		}
		return false;
	}

	public boolean containsNumberGreaterThan(Double number) {
		for (Object o : this) {
			if (o instanceof Number) {
				if ((Double) o > number)
					return true;
			} else if (o instanceof SealMetadata) {
				SealMetadata obj = (SealMetadata) o;
				if (tryParseDouble(obj.getValue())) {
					if (Double.parseDouble(obj.getValue()) > number) {
						return true;
					}
				}
			}
		}
		return false;
	}

	public boolean containsNumberLessThanOrEquals(Double number) {
		for (Object o : this) {
			if (o instanceof Number) {
				if ((Double) o <= number)
					return true;
			} else if (o instanceof SealMetadata) {
				SealMetadata obj = (SealMetadata) o;
				if (tryParseDouble(obj.getValue())) {
					if (Double.parseDouble(obj.getValue()) <= number) {
						return true;
					}
				}
			}
		}
		return false;
	}

	public boolean containsNumberGreaterThanOrEquals(Double number) {
		for (Object o : this) {
			if (o instanceof Number) {
				if ((Double) o >= number)
					return true;
			} else if (o instanceof SealMetadata) {
				SealMetadata obj = (SealMetadata) o;
				if (tryParseDouble(obj.getValue())) {
					if (Double.parseDouble(obj.getValue()) >= number) {
						return true;
					}
				}
			}
		}
		return false;
	}

	public boolean containsDateEquals(Date date) {
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
		String dateAsString = "";
		Date dateFromString = null;
		String inputDateAsString = "";
		Date inputDateFromString = null;

		for (Object o : this) {
			if (o instanceof Date) {
				try {
					dateAsString = simpleDateFormat.format((Date) o);
					dateFromString = simpleDateFormat.parse(dateAsString);

					inputDateAsString = simpleDateFormat.format(date);
					inputDateFromString = simpleDateFormat.parse(inputDateAsString);

					if (dateFromString.compareTo(inputDateFromString) == 0)
						return true;
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else if (o instanceof SealMetadata) {
				SealMetadata obj = (SealMetadata) o;
				if (tryParseDate(obj.getValue())) {
					try {
						inputDateAsString = simpleDateFormat.format(date);
						inputDateFromString = simpleDateFormat.parse(inputDateAsString);

						dateFromString = simpleDateFormat.parse(obj.getValue());

						if (dateFromString.compareTo(inputDateFromString) == 0)
							return true;
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
		return false;
	}

	public boolean containsDateNotEquals(Date date) {
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
		String dateAsString = "";
		Date dateFromString = null;
		String inputDateAsString = "";
		Date inputDateFromString = null;

		for (Object o : this) {
			if (o instanceof Date) {
				try {
					dateAsString = simpleDateFormat.format((Date) o);
					dateFromString = simpleDateFormat.parse(dateAsString);

					inputDateAsString = simpleDateFormat.format(date);
					inputDateFromString = simpleDateFormat.parse(inputDateAsString);

					if (dateFromString.compareTo(inputDateFromString) != 0)
						return true;
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else if (o instanceof SealMetadata) {
				SealMetadata obj = (SealMetadata) o;
				if (tryParseDate(obj.getValue())) {
					try {
						inputDateAsString = simpleDateFormat.format(date);
						inputDateFromString = simpleDateFormat.parse(inputDateAsString);

						dateFromString = simpleDateFormat.parse(obj.getValue());

						if (dateFromString.compareTo(inputDateFromString) != 0)
							return true;
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
		return false;
	}

	public boolean containsDateLessThan(Date date) {
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
		String dateAsString = "";
		Date dateFromString = null;
		String inputDateAsString = "";
		Date inputDateFromString = null;

		for (Object o : this) {
			if (o instanceof Date) {
				try {
					dateAsString = simpleDateFormat.format((Date) o);
					dateFromString = simpleDateFormat.parse(dateAsString);

					inputDateAsString = simpleDateFormat.format(date);
					inputDateFromString = simpleDateFormat.parse(inputDateAsString);

					if (dateFromString.compareTo(inputDateFromString) < 0)
						return true;
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else if (o instanceof SealMetadata) {
				SealMetadata obj = (SealMetadata) o;
				if (tryParseDate(obj.getValue())) {
					try {
						inputDateAsString = simpleDateFormat.format(date);
						inputDateFromString = simpleDateFormat.parse(inputDateAsString);

						dateFromString = simpleDateFormat.parse(obj.getValue());

						if (dateFromString.compareTo(inputDateFromString) < 0)
							return true;
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
		return false;
	}

	public boolean containsDateGreaterThan(Date date) {
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
		String dateAsString = "";
		Date dateFromString = null;
		String inputDateAsString = "";
		Date inputDateFromString = null;

		for (Object o : this) {
			if (o instanceof Date) {
				try {
					dateAsString = simpleDateFormat.format((Date) o);
					dateFromString = simpleDateFormat.parse(dateAsString);

					inputDateAsString = simpleDateFormat.format(date);
					inputDateFromString = simpleDateFormat.parse(inputDateAsString);

					if (dateFromString.compareTo(inputDateFromString) > 0)
						return true;
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else if (o instanceof SealMetadata) {
				SealMetadata obj = (SealMetadata) o;
				if (tryParseDate(obj.getValue())) {
					try {
						inputDateAsString = simpleDateFormat.format(date);
						inputDateFromString = simpleDateFormat.parse(inputDateAsString);

						dateFromString = simpleDateFormat.parse(obj.getValue());

						if (dateFromString.compareTo(inputDateFromString) > 0)
							return true;
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
		return false;
	}

	public boolean containsDateLessThanOrEquals(Date date) {
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
		String dateAsString = "";
		Date dateFromString = null;
		String inputDateAsString = "";
		Date inputDateFromString = null;

		for (Object o : this) {
			if (o instanceof Date) {
				try {
					dateAsString = simpleDateFormat.format((Date) o);
					dateFromString = simpleDateFormat.parse(dateAsString);

					inputDateAsString = simpleDateFormat.format(date);
					inputDateFromString = simpleDateFormat.parse(inputDateAsString);

					if (dateFromString.compareTo(inputDateFromString) <= 0)
						return true;
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else if (o instanceof SealMetadata) {
				SealMetadata obj = (SealMetadata) o;
				if (tryParseDate(obj.getValue())) {
					try {
						inputDateAsString = simpleDateFormat.format(date);
						inputDateFromString = simpleDateFormat.parse(inputDateAsString);

						dateFromString = simpleDateFormat.parse(obj.getValue());

						if (dateFromString.compareTo(inputDateFromString) <= 0)
							return true;
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
		return false;
	}

	public boolean containsDateGreaterThanOrEquals(Date date) {
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
		String dateAsString = "";
		Date dateFromString = null;
		String inputDateAsString = "";
		Date inputDateFromString = null;

		for (Object o : this) {
			if (o instanceof Date) {
				try {
					dateAsString = simpleDateFormat.format((Date) o);
					dateFromString = simpleDateFormat.parse(dateAsString);

					inputDateAsString = simpleDateFormat.format(date);
					inputDateFromString = simpleDateFormat.parse(inputDateAsString);

					if (dateFromString.compareTo(inputDateFromString) >= 0)
						return true;
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else if (o instanceof SealMetadata) {
				SealMetadata obj = (SealMetadata) o;
				if (tryParseDate(obj.getValue())) {
					try {
						inputDateAsString = simpleDateFormat.format(date);
						inputDateFromString = simpleDateFormat.parse(inputDateAsString);

						dateFromString = simpleDateFormat.parse(obj.getValue());

						if (dateFromString.compareTo(inputDateFromString) >= 0)
							return true;
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
		return false;
	}

	public AdvancedList<T> append(T obj) {
		this.add(obj);
		return this;
	}

	public AdvancedList<T> removeObject(T obj) {
		this.remove(obj);
		return this;
	}

	public AdvancedList<T> clearMetadata() {
		this.clear();
		return this;
	}

	public boolean isValidRegEx(String pattern) {
		boolean result = false;
		try {
			Pattern.compile(pattern);
			result = true;
		} catch (PatternSyntaxException patternSyntaxException) {
			patternSyntaxException.printStackTrace();
		}
		return result;
	}

	private boolean tryParseDouble(String value) {
		try {
			Double.parseDouble(value);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	private boolean tryParseDate(String date) {
		try {
			SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
			simpleDateFormat.parse(date);
			return true;
		} catch (ParseException e) {
			return false;
		}
	}
}
