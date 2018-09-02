extern crate serde;
#[macro_use]
extern crate serde_derive;
extern crate serde_json;
extern crate prettytable;
extern crate itertools;
extern crate structopt;
#[macro_use]
extern crate structopt_derive;

use std::io;

use structopt::StructOpt;

mod analyser;
mod ir;

#[derive(StructOpt)]
struct Arguments {
    /// The name of the gate to print a truth table for. If omitted, all truth tables will be
    /// printed.
    gate_name: String,
}

fn main() {
    let args = Arguments::from_args();
    let stdin = io::stdin();

    let graph = serde_json::from_reader(stdin.lock()).unwrap();
    let mut lookups = Default::default();
    let table = analyser::truth_table(&graph, &mut lookups, &args.gate_name);

    let output = analyser::display_truth_table(&graph[&args.gate_name], &table);
    println!("{}", output);
}
