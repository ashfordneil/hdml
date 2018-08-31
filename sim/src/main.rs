#[macro_use] extern crate failure;
extern crate ir;

use failure::Error;
use ir::*;
use std::collections::{HashSet};

// Start from inputs, go out

#[derive(Debug, Fail)]
enum SimError {
    #[fail(display = "rest in peace")]
    Rip,
    #[fail(display = "Bad main")]
    BadMain,
}

#[derive(Debug, Clone, Hash)]
enum SimState {
    Something,
}

fn sim_graph(graph: &Graph, main: String) -> Result<SimState, SimError> {
    let start: &Gate = match graph.gates.get(main) {
        Some(result) => result,
        None => return Err(SimError::BadMain),
    }; 

    loop {
    }
}


fn main() {
}