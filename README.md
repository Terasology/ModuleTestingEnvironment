# ModuleTestingEnvironment

A test helper to instantiate a full headless TerasologyEngine instance

## Usage

Just write a test class that `extends ModuleTestingEnvironment`. You'll be given a TerasologyEngine as `host` and its
in-game context will be exposed as `hostContext`. You can call `host.tick()` to perform a single tick of the main loop,
and background threads will be run automatically.

Bootsrap logic is performed in a `@Before` method, so you will get a new `host` and `hostContext` for each case.

For more examples, see the tests included with this module
