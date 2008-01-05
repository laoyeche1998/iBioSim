//#############################################
//## file: TermCond.java
//## Generated by Byacc/j
//#############################################
package reb2sac;

/**
 * BYACC/J Semantic Value for parser: TermCond This class provides some of the
 * functionality of the yacc/C 'union' directive
 */
public class TermCondVal {
	/**
	 * integer value of this 'union'
	 */
	public int ival;

	/**
	 * double value of this 'union'
	 */
	public double dval;

	/**
	 * string value of this 'union'
	 */
	public String sval;

	/**
	 * object value of this 'union'
	 */
	public Object obj;

	// #############################################
	// ## C O N S T R U C T O R S
	// #############################################
	/**
	 * Initialize me without a value
	 */
	public TermCondVal() {
	}

	/**
	 * Initialize me as an int
	 */
	public TermCondVal(int val) {
		ival = val;
	}

	/**
	 * Initialize me as a double
	 */
	public TermCondVal(double val) {
		dval = val;
	}

	/**
	 * Initialize me as a string
	 */
	public TermCondVal(String val) {
		sval = val;
	}

	/**
	 * Initialize me as an Object
	 */
	public TermCondVal(Object val) {
		obj = val;
	}
}// end class

// #############################################
// ## E N D O F F I L E
// #############################################
