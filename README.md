# ModuleTestingEnvironment

A test helper to instantiate a full headless TerasologyEngine instance

## Usage

Just write a test class that `extends ModuleTestingEnvironment`.

For complete docs please see the
[documentation on Github Pages](https://kaen.github.io/ModuleTestingEnvironment/org/terasology/moduletestingenvironment/ModuleTestingEnvironment.html)

By example:

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

## API

The API should be reasonably well documented, in particular look at the ModuleTestingEnvironment
class and its methods for the provided helpers. 

See [the JavaDoc](https://github.com/kaen/ModuleTestingEnvironment/blob/master/src/main/java/org/terasology/moduletestingenvironment/ModuleTestingEnvironment.java)
or [the tests](https://github.com/kaen/ModuleTestingEnvironment/tree/master/src/test/java/org/terasology/moduletestingenvironment)
for more detailed usage information and examples.

(docs) 