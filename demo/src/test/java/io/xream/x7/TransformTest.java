package io.xream.x7;

import io.xream.x7.demo.MouseRepository;
import io.xream.x7.demo.bean.Mouse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class TransformTest {

    @Autowired
    private MouseRepository repository;

    public boolean testCreate() {

        Mouse mouse = new Mouse();
        mouse.setId(UUID.randomUUID().toString().replace("-","").toUpperCase());
        mouse.setTest("RHR_XXX");

        long id = this.repository.create(mouse);

        return id > 0;
    }
}
