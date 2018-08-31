//! The IR of HDML.
extern crate serde;
#[macro_use]
extern crate serde_derive;

use std::collections::HashSet;

/// A gate
#[derive(Debug, Clone, Hash, Serialize, Deserialize)]
pub struct Gate {
    nodes: Vec<Node>,
    edges: Vec<Edge>,
}

/// A node within a gate
#[derive(Debug, Clone, Hash, Serialize, Deserialize)]
pub struct Node {
    type_: Type,
    name: String,
}

/// An edge on the graph
#[derive(Debug, Clone, Hash, Serialize, Deserialize)]
pub struct Edge {
    /// (name of node, name of output on that node)
    source: (String, String),
    /// (name of node, name of input on that node)
    sink: (String, String),
}

#[derive(Debug, Clone, Hash, Serialize, Deserialize)]
pub enum Type {
    /// An input node - part of the public API of that gate
    Input,
    /// An output node - part of the public API of that gate
    Output,
    /// An internal node - not part of the public API of that gate. Contains within it the gate
    /// type
    Internal(String),
}
