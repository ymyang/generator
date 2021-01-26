package com.ymyang.generator.test;

import com.ymyang.generator.utils.InflectorUtil;
import org.junit.Test;

public class InflectorUtilTest {

	@Test
	public void singularize() {
		String word = "menu";
		String str = InflectorUtil.getInstance().singularize(word);
		System.out.println(str);
	}
}
