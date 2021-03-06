//請一定要import這個Namespace
import junit.framework.*;

//請一定要extends這個TestCase類別
public class TestCalculator extends TestCase {

	public double dInput_1, dInput_2, dResult;
	public Calculator myCal;
	
	//起始單元測試時的方法
	public void setUp() {
		myCal = new Calculator();
	}
	
	//終止單元測試時的方法
	public void tarDown() {
		myCal = null;
	}
	
	/* JUnit會自動找你這個類別裡面，所有開頭為test的方法，並且去執行它。 */
	
	//測試加法一
	public void testAdd_1() {
		dInput_1 = 1.0;
		dInput_2 = 2.0;
		dResult = 3.0;
		assertEquals(dResult, myCal.Add(dInput_1, dInput_2));
	}
	
	//測試加法二
	public void testAdd_2() {
		dInput_1 = 1.1;
		dInput_2 = 2.2;
		dResult = 3.3;
		assertEquals(dResult, myCal.Add(dInput_1, dInput_2),0.01);
	}
	
	//測試減法
	public void testSub() {
		dInput_1 = 3.0;
		dInput_2 = 2.0;
		dResult = 1.0;
		assertEquals(dResult, myCal.Sub(dInput_1, dInput_2));
	}
}