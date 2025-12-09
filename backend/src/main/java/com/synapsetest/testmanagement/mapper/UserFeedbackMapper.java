package com.synapsetest.testmanagement.mapper;

import com.synapsetest.testmanagement.model.UserFeedback;
import com.synapsetest.testmanagement.typehandler.ListStringTypeHandler;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 用户反馈Mapper
 */
@Mapper
public interface UserFeedbackMapper {

    @Insert("INSERT INTO user_feedback " +
            "(id, request_id, rating, feedback_type, comments, accepted_cases, rejected_cases, " +
            "user_id, created_at, updated_at) " +
            "VALUES (#{id}, #{requestId}, #{rating}, #{feedbackType}, #{comments}, " +
            "#{acceptedCases, typeHandler=com.synapsetest.testmanagement.typehandler.ListStringTypeHandler}, " +
            "#{rejectedCases, typeHandler=com.synapsetest.testmanagement.typehandler.ListStringTypeHandler}, " +
            "#{userId}, #{createdAt}, #{updatedAt})")
    int insert(UserFeedback feedback);

    @Select("SELECT * FROM user_feedback WHERE request_id = #{requestId}")
    @Results({
        @Result(property = "acceptedCases", column = "accepted_cases", typeHandler = ListStringTypeHandler.class),
        @Result(property = "rejectedCases", column = "rejected_cases", typeHandler = ListStringTypeHandler.class)
    })
    List<UserFeedback> findByRequestId(String requestId);

    @Select("SELECT * FROM user_feedback WHERE user_id = #{userId} ORDER BY created_at DESC")
    @Results({
        @Result(property = "acceptedCases", column = "accepted_cases", typeHandler = ListStringTypeHandler.class),
        @Result(property = "rejectedCases", column = "rejected_cases", typeHandler = ListStringTypeHandler.class)
    })
    List<UserFeedback> findByUserId(String userId);

    @Select("SELECT * FROM user_feedback WHERE rating >= #{minRating} ORDER BY created_at DESC")
    @Results({
        @Result(property = "acceptedCases", column = "accepted_cases", typeHandler = ListStringTypeHandler.class),
        @Result(property = "rejectedCases", column = "rejected_cases", typeHandler = ListStringTypeHandler.class)
    })
    List<UserFeedback> findByMinRating(@Param("minRating") int minRating);

    @Select("SELECT * FROM user_feedback ORDER BY created_at DESC")
    @Results({
        @Result(property = "acceptedCases", column = "accepted_cases", typeHandler = ListStringTypeHandler.class),
        @Result(property = "rejectedCases", column = "rejected_cases", typeHandler = ListStringTypeHandler.class)
    })
    List<UserFeedback> findAll();

    @Delete("DELETE FROM user_feedback WHERE id = #{id}")
    int deleteById(String id);
}
