#[macro_use] extern crate failure;
extern crate ir;

use failure::Error;
use ir::*;

// Start from inputs, go out

#[derive(Debug, Fail)]
enum SimError {
    #[fail(display = "rest in peace")]
    Rip,
}

#[derive(Debug, Clone, Hash, Serialize, Deserialize)]
enum SimState {
    Something,
}

fn simulate_gate(gate: &Gate, inputs: bool) -> Result<SimState, SimError> {
    Ok(SimState::Something)
}


fn main() {
    let graph = Graph {
        nodes: vec![],
        edges: vec![],
    };
}