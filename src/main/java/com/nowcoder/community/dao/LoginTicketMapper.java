package com.nowcoder.community.dao;

import com.nowcoder.community.entity.LoginTicket;
import org.apache.ibatis.annotations.*;

@Mapper
@Deprecated
public interface LoginTicketMapper {

    //插入LoginTicket对象
    @Insert({
            "insert into login_ticket(user_id, ticket, status, expired) ",
            "values(#{userId}, #{ticket}, #{status}, #{expired})"
    })
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertLoginTicket(LoginTicket loginTicket);


    //通过唯一标识ticket查询一条记录
    @Select({
            "select id, user_id, ticket, status, expired ",
            "from login_ticket where ticket = #{ticket}"
    })
    LoginTicket selectByTicket(String ticket);


    //更新有效状态 0-有效 1-无效
    @Update({
            "update login_ticket set status = #{status} ",
            "where ticket = #{ticket}"
    })
    int updateStatus(String ticket, int status);
}
