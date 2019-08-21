package com.nowcoder.community;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.HashMap;
import java.util.Map;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class CommunityApplicationTests {

	@Test
	public void contextLoads() {
		String s = "dsfsf209rj21";
		System.out.println(s.substring(0,3));
	}
	
	@Test
	public void testMap(){
		Map<String, Object> map = new HashMap<>();
		map.put("1", 1);
		System.out.println(map.get("2"));
	}

}
