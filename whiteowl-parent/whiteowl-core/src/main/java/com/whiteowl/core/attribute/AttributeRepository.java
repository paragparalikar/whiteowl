package com.whiteowl.core.attribute;

import org.socialsignin.spring.data.dynamodb.repository.EnableScan;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
@EnableScan
public interface AttributeRepository extends CrudRepository<Attribute, String> {

}
