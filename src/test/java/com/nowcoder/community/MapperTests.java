package com.nowcoder.community;

import org.junit.Test;
import com.nowcoder.community.dao.LoginTicketMapper;
import com.nowcoder.community.entity.LoginTicket;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Date;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class MapperTests {

    @Autowired
    private LoginTicketMapper loginTicketMapper;

    @Test
    public void testInsertLoginTicket() {
        LoginTicket loginTicket = new LoginTicket();
        loginTicket.setUserId(1);
        loginTicket.setTicket("test1");
        loginTicket.setStatus(1);
        loginTicket.setExpired(new Date(System.currentTimeMillis()));
        loginTicketMapper.insertLoginTicket(loginTicket);
        System.out.println(loginTicket);
    }

    @Test
    public void testSelectByTicket() {
        LoginTicket loginTicket = loginTicketMapper.selectByTicket("test");
        System.out.println(loginTicket);
    }

    @Test
    public void testUpdateStatus() {
        loginTicketMapper.updateStatus("test", 0);
        LoginTicket loginTicket = loginTicketMapper.selectByTicket("test");
        System.out.println(loginTicket);
    }

}
