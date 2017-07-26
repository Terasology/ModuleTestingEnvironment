# ModuleTestingEnvironment

A test helper to instantiate a full headless TerasologyEngine instance

## Usage

Just write a test class that `extends ModuleTestingEnvironment`.

For complete docs please see the
[documentation on Github Pages](https://kaen.github.io/ModuleTestingEnvironment/org/terasology/moduletestingenvironment/ModuleTestingEnvironment.html)

For more examples see
[the test suite](https://github.com/kaen/ModuleTestingEnvironment/tree/master/src/test/java/org/terasology/moduletestingenvironment)

Here's an example taken from a Module I'm writing myself:

```java
public class MyModuleEngineTest extends ModuleTestingEnvironment {
    protected Logger logger = Logger.getLogger(MyModuleEngineTest.class.getName());
    protected EntityManager entityManager;

    @Override
    public Set<String> getDependencies() {
        return Sets.newHashSet("MyModule");
    }

    @Override
    public String getWorldGeneratorUri() {
        return "mymodule:mymodulesworldgenerator";
    }

    @Before
    public void beforeMyModuleTests() {
        entityManager = getHostContext().get(EntityManager.class);
        runUntil(()-> getHostContext().get(MyModuleReadySystem.class).isMyModuleReady());
    }
    
    @Test
    public void testMyModule() {
        // find sites and request they be loaded in
        for (EntityRef site : entityManager.getEntitiesWith(SiteComponent.class)) {
            LocationComponent locationComponent = site.getComponent(LocationComponent.class);
            forceAndWaitForGeneration(new Vector3i(locationComponent.getWorldPosition()));
        }
        
        // create a mortal and wait for it to find a settlement through behavior logic
        EntityRef subject = entityManager.create(Assets.getPrefab("mymodule:mortal").get());
        MortalComponent mortalComponent = subject.getComponent(MortalComponent.class);
        runUntil(()->mortalComponent.settlement != null);
    }
}
```
