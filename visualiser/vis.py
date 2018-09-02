#!/usr/bin/env python
import json
import networkx as nx
import matplotlib.pyplot as plt
from graphviz import Source, render
import sys

if len(sys.argv) == 2:
    f = open(sys.argv[1])
    json_data = "".join(f.readlines())
else:
    # Read from stdin
    json_data = "".join(sys.stdin.readlines())

python_data = json.loads(json_data)

# print(python_data)
circuit_graphs = []

for key in python_data:
    circuit = python_data[key]
    G = nx.MultiDiGraph(name=key)

    # Add nodes
    for node in circuit["nodes"]:
        node_details = circuit["nodes"][node]
        node_type = node_details["type_"] if node_details["type_"] == "Input" or node_details["type_"] == "Output" else node_details["type_"]["Internal"]
        G.add_node(node, label=("<<TABLE><TR><TD>{}</TD></TR><TR><TD><B>{}</B></TD></TR></TABLE>>".format(node, node_type)))
    
    # Add edges
    for edge in circuit["edges"]:
        for outgoing in circuit["edges"][edge]:
            G.add_edge(outgoing["source"], outgoing["sink"][0], label=outgoing["sink"][1])

    # Remove unused nodes
    to_remove = []
    for node in G.nodes():
        if nx.degree(G, node) == 0:
            to_remove.append(node)
    for node in to_remove:
        G.remove_node(node)

    circuit_graphs.append(G)

for graph in circuit_graphs:
    dot = nx.nx_pydot.to_pydot(graph)
    src = Source(dot)
    src.render(graph.graph["name"], view=False, cleanup=True)
