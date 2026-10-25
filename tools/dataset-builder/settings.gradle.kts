// Standalone settings for the dataset-builder tool module.
//
// This lets the module be built and run on its own, without the parent build:
//   cd tools/dataset-builder && gradle run --args="..."
//
// When the parent build integrates this module via `include(":tools:dataset-builder")`
// in the root settings, Gradle uses the root settings file and ignores this one.
// (If the parent prefers a conflict-free layout, it may delete this file.)
rootProject.name = "dataset-builder"
