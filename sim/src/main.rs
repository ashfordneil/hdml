#[macro_use] extern crate failure;
extern crate ir;

use failure::Error;
use ir::*;
use std::collections::{HashSet, HashMap};

#[derive(Debug, Clone, Hash)]
enum SimState {
    Something,
}

fn sim_graph(graph: &Graph, main: String) -> Result<SimState, Error> {
    let start: &Gate = match graph.gates.get(main) {
        Some(result) => result,
        None => bail!("Bad main"),
    }; 

    let visited: HashSet<String> = HashSet::new();

    for node in inputs {
        let node: &Node = match graph.nodes.get(node) {
            Some(result) => result,
            None => bail!("Bad input node"),
        }; 

        let next: &Node = 
    }
}


fn main() {
    let mut graph: HashMap<String, Gate> = HashMap::new();
    let mut state: HashMap<String, Option<bool>> = HashMap::new();

    for (_, gate) in graph.iter() {
        state.insert(gate, None);
    }

    sim_graph(graph, "main".into());
}