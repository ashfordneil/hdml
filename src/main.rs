extern crate serde;
#[macro_use]
extern crate serde_derive;
extern crate serde_json;

extern crate itertools;

mod analyser;
mod ir;

fn main() {
    let graph = serde_json::from_str(include_str!("latch.json")).unwrap();
    let mut lookups = Default::default();
    let table = analyser::truth_table(&graph, &mut lookups, "xor");

    let output = analyser::display_truth_table(&graph["xor"], &table);
    println!("{}", output);
}
