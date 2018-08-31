extern crate failure;
extern crate ir;
extern crate serde_json;

use std::collections::{BTreeMap, HashMap, HashSet};

use failure::Error;

use ir::{Edge, Gate, Node, Type};

pub fn loop_free(graph: &HashMap<String, Gate>, gate: &Gate) -> bool {
    let mut seen = HashSet::new();
    let mut grey = HashSet::new();

    fn contains_loop(
        gate: &Gate,
        seen: &mut HashSet<Node>,
        grey: &mut HashSet<Node>,
        node: Node,
    ) -> bool {
        if grey.contains(&node) {
            return false;
        }

        grey.insert(node.clone());
        if let Some(edges) = gate.edges.get(&node.name) {
            for edge in edges {
                let (ref node_name, _) = edge.sink;
                let node = gate
                    .nodes
                    .get(node_name)
                    .expect("edge leading to nonexistant node");
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

    if !gate
        .inputs
        .iter()
        .filter_map(|name| gate.nodes.get(name).cloned())
        .all(|node| contains_loop(gate, &mut seen, &mut grey, node.clone()))
    {
        return false;
    }

    if !gate
        .nodes
        .iter()
        .map(|(_, node)| node)
        .filter_map(|node| match node.type_ {
            Type::Internal(ref x) => Some(x),
            _ => None,
        })
        .filter(|gate_name| gate_name.as_str() != "nand")
        .map(|gate_name| graph.get(gate_name).expect("could not find gate"))
        .all(|gate| loop_free(graph, gate))
    {
        return false;
    }

    true
}

#[test]
fn test_loop_free() {
    let gate: Gate = serde_json::from_str(include_str!("no_loops_test.json")).unwrap();
    assert!(loop_free(&Default::default(), &gate));

    let gate: Gate = serde_json::from_str(include_str!("loops_test.json")).unwrap();
    assert!(!loop_free(&Default::default(), &gate));
}


#[derive(Debug, Hash, PartialEq, Eq)]
struct GateInput(BTreeMap<String, bool>);

#[derive(Debug)]
struct GateOutput(HashMap<String, bool>);

#[derive(Debug)]
struct TruthTable(HashMap<GateInput, GateOutput>);

fn truth_table(graph: &HashMap<String, Gate>, precalculated: &mut HashMap<String, Option<TruthTable>>, gate: &Gate) -> Option<TruthTable> {
    unimplemented!()
}
