extern crate failure;
extern crate ir;
extern crate serde_json;

use std::collections::{HashSet, HashMap};

use failure::Error;

use ir::{Edge, Gate, Node, Type};

pub fn loop_free(graph: &HashMap<String, Gate>, gate: &Gate) -> Result<bool, Error> {
    let mut inputs = gate.nodes.iter().map(|(_, x)| x).filter(|node| node.type_ == Type::Input);
    let mut seen = HashSet::new();
    let mut grey = HashSet::new();

    fn contains_loop(gate: &Gate, seen: &mut HashSet<Node>, grey: &mut HashSet<Node>, node: Node) -> bool {
        if grey.contains(&node) {
            return false;
        }

        grey.insert(node.clone());
        if let Some(edges) = gate.edges.get(&node) {
            for edge in edges {
                let (ref node_name, _) = edge.sink;
                let node = gate.nodes.get(node_name).expect("edge leading to nonexistant node");
                if seen.contains(&node) {
                    continue;
                }
                if !contains_loop(gate, seen, grey, node.clone()) {
                    return false;
                }
            }
        }

        grey.remove(&node);
        seen.insert(node);
        
        true
    }

    if !inputs.all(|node| contains_loop(gate, &mut seen, &mut grey, node.clone())) {
        return Ok(false);
    }

    Ok(true)
}

#[test]
fn test_loop_free() {
    let gate: Gate = serde_json::from_str(include_str!("no_loops_test.json")).unwrap();
}
