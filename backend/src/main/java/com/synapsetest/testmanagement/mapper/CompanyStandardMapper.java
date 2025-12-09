package com.synapsetest.testmanagement.mapper;

import com.synapsetest.testmanagement.model.CompanyStandard;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 公司标准Mapper
 */
@Mapper
public interface CompanyStandardMapper {

    /**
     * 插入公司标准
     */
    @Insert("INSERT INTO company_standards " +
            "(id, name, description, category, standard_data, version, status, created_at, updated_at) " +
            "VALUES (#{id}, #{name}, #{description}, #{category}, #{standardData}, " +
            "#{version}, #{status}, #{createdAt}, #{updatedAt})")
    int insert(CompanyStandard standard);

    /**
     * 根据ID查询
     */
    @Select("SELECT * FROM company_standards WHERE id = #{id}")
    CompanyStandard findById(String id);

    /**
     * 根据名称查询
     */
    @Select("SELECT * FROM company_standards WHERE name = #{name}")
    CompanyStandard findByName(String name);

    /**
     * 根据类别查询
     */
    @Select("SELECT * FROM company_standards WHERE category = #{category}")
    List<CompanyStandard> findByCategory(String category);

    /**
     * 根据状态查询
     */
    @Select("SELECT * FROM company_standards WHERE status = #{status} ORDER BY created_at DESC")
    List<CompanyStandard> findByStatus(String status);

    /**
     * 查询所有标准
     */
    @Select("SELECT * FROM company_standards ORDER BY created_at DESC")
    List<CompanyStandard> findAll();

    /**
     * 更新公司标准
     */
    @Update("UPDATE company_standards SET name=#{name}, description=#{description}, " +
            "category=#{category}, standard_data=#{standardData}, version=#{version}, " +
            "status=#{status}, updated_at=#{updatedAt} WHERE id=#{id}")
    int update(CompanyStandard standard);

    /**
     * 根据ID删除
     */
    @Delete("DELETE FROM company_standards WHERE id = #{id}")
    int deleteById(String id);
}
